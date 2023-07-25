package com.bollu.utils.goosefs.config;

import org.apache.commons.codec.binary.Hex;

import javax.annotation.concurrent.ThreadSafe;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ThreadSafe
public class Hash {
  private final MessageDigest mMD5;
  private final Supplier<Stream<byte[]>> mProperties;
  private final AtomicBoolean mShouldUpdate;
  private volatile String mVersion;

  public Hash(Supplier<Stream<byte[]>> properties) {
    try {
      mMD5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    mProperties = properties;
    mShouldUpdate = new AtomicBoolean(true);
  }

  public void markOutdated() {
    mShouldUpdate.set(true);
  }

  private String compute() {
    mMD5.reset();
    mProperties.get().forEach(property -> mMD5.update(property));
    return Hex.encodeHexString(mMD5.digest());
  }

  public String get() {
    if (mShouldUpdate.get()) {
      synchronized (this) {
        // If another thread has recomputed the version, no need to recompute again.
        if (mShouldUpdate.get()) {
          mVersion = compute();
          mShouldUpdate.set(false);
        }
      }
    }
    return mVersion;
  }

}
