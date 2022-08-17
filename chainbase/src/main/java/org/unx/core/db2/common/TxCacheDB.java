package org.unx.core.db2.common;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Longs;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.iq80.leveldb.WriteOptions;
import org.unx.common.parameter.CommonParameter;
import org.unx.common.storage.leveldb.LevelDbDataSourceImpl;
import org.unx.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.unx.common.utils.ByteArray;
import org.unx.common.utils.JsonUtil;
import org.unx.common.utils.StorageUtils;
import org.unx.core.capsule.BytesCapsule;
import org.unx.core.db.RecentTransactionItem;
import org.unx.core.db.RecentTransactionStore;
import org.unx.core.db.common.iterator.DBIterator;

@Slf4j(topic = "DB")
public class TxCacheDB implements DB<byte[], byte[]>, Flusher {

  // > 65_536(= 2^16) blocks, that is the number of the reference block
  private final int BLOCK_COUNT = 70_000;
  private final long MAX_SIZE = 65536;
  private Map<Key, Long> db = new WeakHashMap<>();
  private Multimap<Long, Key> blockNumMap = ArrayListMultimap.create();
  private String name;
  private RecentTransactionStore recentTransactionStore;

  // add a persistent storage, the store name is: trans-cache
  // when fullnode startup, transactionCache initializes transactions from this store
  private DB<byte[], byte[]> persistentStore;

  public TxCacheDB(String name, RecentTransactionStore recentTransactionStore) {
    this.name = name;
    this.recentTransactionStore = recentTransactionStore;
    int dbVersion = CommonParameter.getInstance().getStorage().getDbVersion();
    String dbEngine = CommonParameter.getInstance().getStorage().getDbEngine();
    if (dbVersion == 2) {
      if ("LEVELDB".equals(dbEngine.toUpperCase())) {
        this.persistentStore = new LevelDB(
                new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(name),
                        name, StorageUtils.getOptionsByDbName(name),
                        new WriteOptions().sync(CommonParameter.getInstance()
                                .getStorage().isDbSync())));
      } else if ("ROCKSDB".equals(dbEngine.toUpperCase())) {
        String parentPath = Paths
                .get(StorageUtils.getOutputDirectoryByDbName(name), CommonParameter
                        .getInstance().getStorage().getDbDirectory()).toString();

        this.persistentStore = new RocksDB(
                new RocksDbDataSourceImpl(parentPath,
                        name, CommonParameter.getInstance()
                        .getRocksDBCustomSettings()));
      } else {
        throw new RuntimeException("db type is not supported.");
      }
    } else {
      throw new RuntimeException("db version is not supported.");
    }
    init();
  }

  /**
   * this method only used for init, put all data in tran-cache into the two maps.
   */
  private void initCache() {
    long start = System.currentTimeMillis();
    DBIterator iterator = (DBIterator) persistentStore.iterator();
    while (iterator.hasNext()) {
      Entry<byte[], byte[]> entry = iterator.next();
      byte[] key = entry.getKey();
      byte[] value = entry.getValue();
      if (key == null || value == null) {
        return;
      }
      Key k = Key.copyOf(key);
      Long v = Longs.fromByteArray(value);
      blockNumMap.put(v, k);
      db.put(k, v);
    }
    logger.info("Transaction cache init-1 db-size:{}, blockNumMap-size:{}, cost:{}ms",
            db.size(), blockNumMap.size(), System.currentTimeMillis() - start);
  }

  private void init() {
    long size = recentTransactionStore.size();
    if (size != MAX_SIZE) {
      initCache();
      return;
    }

    long start = System.currentTimeMillis();
    List<byte[]> l1 = new ArrayList<>();
    List<RecentTransactionItem> l2 = new ArrayList<>();
    Iterator<Entry<byte[], BytesCapsule>> iterator = recentTransactionStore.iterator();
    while (iterator.hasNext()) {
      l1.add(iterator.next().getValue().getData());
    }

    l1.forEach(v -> l2.add(JsonUtil.json2Obj(new String(v), RecentTransactionItem.class)));

    l2.forEach(v -> v.getTransactionIds().forEach(tid -> putTransaction(tid, v.getNum())));

    logger.info("Transaction cache init-2 db-size:{}, blockNumMap-size:{}, cost:{}ms",
            db.size(), blockNumMap.size(), System.currentTimeMillis() - start);
  }

  private void putTransaction(String key, long value) {
    Key k = Key.copyOf(Hex.decode(key));
    Long v = Longs.fromByteArray(ByteArray.fromLong(value));
    blockNumMap.put(v, k);
    db.put(k, v);
  }


  @Override
  public byte[] get(byte[] key) {
    Long v = db.get(Key.of(key));
    return v == null ? null : Longs.toByteArray(v);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    if (key == null || value == null) {
      return;
    }

    Key k = Key.copyOf(key);
    Long v = Longs.fromByteArray(value);
    blockNumMap.put(v, k);
    db.put(k, v);
    // put the data into persistent storage
    persistentStore.put(key, value);
    removeEldest();
  }

  private void removeEldest() {
    Set<Long> keys = blockNumMap.keySet();
    if (keys.size() > BLOCK_COUNT) {
      keys.stream()
              .min(Long::compareTo)
              .ifPresent(k -> {
                Collection<Key> unxHashs = blockNumMap.get(k);
                // remove transaction from persistentStore,
                // if foreach is inefficient, change remove-foreach to remove-batch
                unxHashs.forEach(key -> persistentStore.remove(key.getBytes()));
                blockNumMap.removeAll(k);
                logger.debug("******removeEldest block number:{}, block count:{}", k, keys.size());
              });
    }
  }

  @Override
  public long size() {
    return db.size();
  }

  @Override
  public boolean isEmpty() {
    return db.isEmpty();
  }

  @Override
  public void remove(byte[] key) {
    if (key != null) {
      db.remove(Key.of(key));
    }
  }

  @Override
  public String getDbName() {
    return name;
  }

  @Override
  public Iterator<Entry<byte[], byte[]>> iterator() {
    return Iterators.transform(db.entrySet().iterator(),
            e -> Maps.immutableEntry(e.getKey().getBytes(), Longs.toByteArray(e.getValue())));
  }

  @Override
  public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    batch.forEach((k, v) -> this.put(k.getBytes(), v.getBytes()));
  }

  @Override
  public void close() {
    reset();
    db = null;
    blockNumMap = null;
    persistentStore.close();
  }

  @Override
  public void reset() {
    db.clear();
    blockNumMap.clear();
  }

  @Override
  public TxCacheDB newInstance() {
    return new TxCacheDB(name, recentTransactionStore);
  }

  @Override
  public void stat() {
    this.persistentStore.stat();
  }
}
