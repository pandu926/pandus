package org.unx.common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropUtil {

  public static String readProperty(String file, String key) {
    InputStream is = null;
    FileInputStream fis = null;
    Properties prop = null;
    try {
      prop = new Properties();
      fis = new FileInputStream(file);
      is = new BufferedInputStream(fis);
      prop.load(is);
      String value = new String(prop.getProperty(key, "").getBytes("ISO-8859-1"), "UTF-8");
      return value;
    } catch (Exception e) {
      logger.error("{}", e);
      return "";
    } finally {
      if (prop != null) {
        prop = null;
      }
      //fis
      try {
        if (fis != null) {
          fis.close();
          fis = null;
        }
      } catch (Exception e) {
        logger.warn("{}", e);
      }
      //is
      try {
        if (is != null) {
          is.close();
          is = null;
        }
      } catch (Exception e) {
        logger.error("{}", e);
      }
    }
  }

  public static boolean writeProperty(String file, String key, String value) {
    FileInputStream fis = null;
    Properties properties = new Properties();
    OutputStream out = null;
    BufferedWriter bw = null;
    try {
      fis = new FileInputStream(file);
      BufferedReader bf = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
      properties.load(bf);
      out = new FileOutputStream(file);
      bw = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
      properties.setProperty(key, value);
      properties.store(bw, "Generated by the application.  PLEASE DO NOT EDIT! ");
    } catch (Exception e) {
      logger.warn("{}", e);
      return false;
    } finally {
      if (properties != null) {
        properties = null;
      }
      //fis
      try {
        if (fis != null) {
          fis.close();
          fis = null;
        }
      } catch (Exception e) {
        logger.warn("{}", e);
      }
      //out
      try {
        if (out != null) {
          out.close();
          out = null;
        }
      } catch (IOException e) {
        logger.warn("{}", e);
      }
      // bw
      try {
        if (bw != null) {
          bw.close();
          bw = null;
        }
      } catch (Exception e) {
        logger.error("{}", e);
      }
    }
    return true;
  }

}