package com.bollu.goosefs.resource.lock;

import com.google.common.base.Preconditions;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RWLockResource extends LockResource{

  private final ReentrantReadWriteLock mRwLock;

  public RWLockResource(ReentrantReadWriteLock rwLock, LockMode mode, boolean acquireLock,
                        boolean useTryLock) {
    super(mode == LockMode.READ ? rwLock.readLock() : rwLock.writeLock(), acquireLock, useTryLock);
    mRwLock = rwLock;
  }


  //降级操作必须在同一个线程进行

  /**
   * 线程进入读锁的前提条件：
   *
   * 没有其他线程的写锁，
   *
   * 没有写请求或者有写请求，但调用线程和持有锁的线程是同一个。
   *
   * 线程进入写锁的前提条件：
   *
   * 没有其他线程的读锁
   *
   * 没有其他线程的写锁
   *
   * 而读写锁有以下三个重要的特性：
   *
   * （1）公平选择性：支持非公平（默认）和公平的锁获取方式，吞吐量还是非公平优于公平。
   *
   * （2）重进入：读锁和写锁都支持线程重进入。
   *
   * （3）锁降级：遵循获取写锁、获取读锁再释放写锁的次序，写锁能够降级成为读锁。
   * @return
   */
  public boolean downgrade() {
    if (!mRwLock.isWriteLocked()) {
      return false;
    }
    Preconditions.checkState(mRwLock.isWriteLockedByCurrentThread(),
        "Lock downgrades may only be initiated by the holding thread.");
    Preconditions.checkState(mLock == mRwLock.writeLock(), "mLock must be the same as mRwLock");
    // Downgrade by taking the read lock and then unlocking the write lock.
    mRwLock.readLock().lock();
    mLock.unlock();
    mLock = mRwLock.readLock();
    return true;
  }
}

