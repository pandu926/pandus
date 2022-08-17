package org.unichain.core.db.store;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.unichain.common.utils.Utils;
import org.unichain.core.capsule.ProtoCapsule;
import org.unichain.core.config.args.Args;
import org.unichain.core.db.RevokingDatabase;
import org.unichain.core.db.api.IndexHelper;
import org.unichain.core.db.common.DataPage;
import org.unichain.core.db2.common.DB;
import org.unichain.core.db2.common.IRevokingDB;
import org.unichain.core.db2.common.LevelDB;
import org.unichain.core.db2.common.RocksDB;
import org.unichain.core.db2.core.IUnichainChainBase;
import org.unichain.core.db2.core.RevokingDBWithCachingNewValue;
import org.unichain.core.exception.BadItemException;
import org.unichain.core.exception.ItemNotFoundException;

import javax.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j(topic = "DB")
public abstract class UnichainStoreWithRevoking<T extends ProtoCapsule> implements IUnichainChainBase<T> {

  @Getter // only for unit test
  protected IRevokingDB revokingDB;

  private TypeToken<T> token = new TypeToken<T>(getClass()) {
  };

  @Autowired
  private RevokingDatabase revokingDatabase;
  @Autowired(required = false)
  protected IndexHelper indexHelper;
  @Getter
  private String dbName;

  protected UnichainStoreWithRevoking(String dbName) {
    this.dbName = dbName;
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    String dbEngine = Args.getInstance().getStorage().getDbEngine();
    if (dbVersion == 2) {
      if ("LEVELDB".equalsIgnoreCase(dbEngine)) {
        this.revokingDB = new RevokingDBWithCachingNewValue(dbName, LevelDB.class);
      } else if ("ROCKSDB".equalsIgnoreCase(dbEngine)) {
        this.revokingDB = new RevokingDBWithCachingNewValue(dbName, RocksDB.class);
      }
    } else {
      throw new RuntimeException("db version is only 2.(" + dbVersion + ")");
    }
  }

  protected UnichainStoreWithRevoking(String dbName, Class<? extends DB> clz) {
    this.dbName = dbName;
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    if (dbVersion == 2) {
      this.revokingDB = new RevokingDBWithCachingNewValue(dbName, clz);
    } else {
      throw new RuntimeException("db version is only 2.(" + dbVersion + ")");
    }
  }

  @PostConstruct
  private void init() {
    revokingDatabase.add(revokingDB);
  }

  @Override
  public void put(byte[] key, T item) {
    if (Objects.isNull(key) || Objects.isNull(item)) {
      return;
    }

    revokingDB.put(key, item.getData());
  }

  @Override
  public void delete(byte[] key) {
    revokingDB.delete(key);
  }

  @Override
  public T get(byte[] key) throws ItemNotFoundException, BadItemException {
    return of(revokingDB.get(key));
  }

  @Override
  public T getUnchecked(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);

    try {
      return of(value);
    } catch (BadItemException e) {
      return null;
    }
  }

  public T of(byte[] value) throws BadItemException {
    try {
      Constructor constructor = token.getRawType().getConstructor(byte[].class);
      @SuppressWarnings("unchecked")
      T t = (T) constructor.newInstance((Object) value);
      return t;
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new BadItemException(e.getMessage());
    }
  }

  @Override
  public boolean has(byte[] key) {
    return revokingDB.has(key);
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void close() {
    revokingDB.close();
  }

  @Override
  public void reset() {
    revokingDB.reset();
  }

  @Override
  public Iterator<Map.Entry<byte[], T>> iterator() {
    return Iterators.transform(revokingDB.iterator(), e -> {
      try {
        return Maps.immutableEntry(e.getKey(), of(e.getValue()));
      } catch (BadItemException e1) {
        throw new RuntimeException(e1);
      }
    });
  }

  public long size() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  public void setMode(boolean mode) {
    revokingDB.setSnapshotMode(mode);
  }

  public List<T> getAll() {
    return Streams.stream(iterator())
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
  }

  public DataPage<T> getDataPage(int pageSize, int pageIndex){
    var all = getAll();
    return new DataPage<>(pageIndex, pageSize, all.size(), Utils.paging(all, pageIndex, pageSize));
  }
}
