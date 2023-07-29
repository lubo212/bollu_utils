package com.bollu.goosefs.common.utils;

import com.bollu.goosefs.common.constant.Constants;
import com.bollu.goosefs.common.options.WaitForOptions;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

public class CommonUtils {
  private static final Logger LOG = LoggerFactory.getLogger(CommonUtils.class);

  private static final String ALPHANUM =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  private static final Random RANDOM = new Random();

  private static final int JAVA_MAJOR_VERSION =
      parseMajorVersion(System.getProperty("java.version"));

  /**
   * Convenience method for calling {@link #createProgressThread(long, PrintStream)} with an
   * interval of 2 seconds.
   *
   * @param stream the print stream to write to
   * @return the thread
   */
  public static Thread createProgressThread(PrintStream stream) {
    return createProgressThread(2L * Constants.SECOND_MS, stream);
  }

  /**
   * Creates a thread which will write "." to the given print stream at the given interval. The
   * created thread is not started by this method. The created thread will be a daemon thread
   * and will halt when interrupted.
   *
   * @param intervalMs the time interval in milliseconds between writes
   * @param stream the print stream to write to
   * @return the thread
   */
  public static Thread createProgressThread(final long intervalMs, final PrintStream stream) {
    Thread t = new Thread(() -> {
      while (true) {
        CommonUtils.sleepMs(intervalMs);
        if (Thread.interrupted()) {
          return;
        }
        stream.print(".");
      }
    });
    t.setDaemon(true);
    return t;
  }

  public static String getTmpDir(List<String> tmpDirs) {
    Preconditions.checkState(!tmpDirs.isEmpty(), "No temporary directories available");
    if (tmpDirs.size() == 1) {
      return tmpDirs.get(0);
    }
    // Use existing random instead of ThreadLocal because contention is not expected to be high.
    return tmpDirs.get(RANDOM.nextInt(tmpDirs.size()));
  }

  public static <T> String listToString(List<T> list) {
    StringBuilder sb = new StringBuilder();
    for (T s : list) {
      if (sb.length() != 0) {
        sb.append(" ");
      }
      sb.append(s);
    }
    return sb.toString();
  }

  public static <T> String argsToString(String separator, T... args) {
    StringBuilder sb = new StringBuilder();
    for (T s : args) {
      if (sb.length() != 0) {
        sb.append(separator);
      }
      sb.append(s);
    }
    return sb.toString();
  }

  public static String[] toStringArray(ArrayList<String> src) {
    return src.toArray(new String[0]);
  }

  /**
   * Generates a random alphanumeric string of the given length.
   *
   * @param length the length
   * @return a random string
   */
  public static String randomAlphaNumString(int length) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
    }
    return sb.toString();
  }

  /**
   * Generates a random byte array of the given length.
   *
   * @param length the length
   * @return a random byte array
   */
  public static byte[] randomBytes(int length) {
    byte[] result = new byte[length];
    RANDOM.nextBytes(result);
    return result;
  }

  /**
   * @param version the version string of the JVMgi
   * @return the major version of the current JVM, 8 for 1.8, 11 for java 11
   */
  public static int parseMajorVersion(String version) {
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if (dot != -1) {
        version = version.substring(0, dot);
      }
    }
    return Integer.parseInt(version);
  }

  /**
   * Creates new instance of a class by calling a constructor that receives ctorClassArgs arguments.
   *
   * @param <T> type of the object
   * @param cls the class to create
   * @param ctorClassArgs parameters type list of the constructor to initiate, if null default
   *        constructor will be called
   * @param ctorArgs the arguments to pass the constructor
   * @return new class object
   * @throws RuntimeException if the class cannot be instantiated
   */
  public static <T> T createNewClassInstance(Class<T> cls, Class<?>[] ctorClassArgs,
                                             Object[] ctorArgs) {
    try {
      if (ctorClassArgs == null) {
        return cls.newInstance();
      }
      Constructor<T> ctor = cls.getConstructor(ctorClassArgs);
      return ctor.newInstance(ctorArgs);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Strips the suffix if it exists. This method will leave keys without a suffix unaltered.
   *
   * @param key the key to strip the suffix from
   * @param suffix suffix to remove
   * @return the key with the suffix removed, or the key unaltered if the suffix is not present
   */
  public static String stripSuffixIfPresent(final String key, final String suffix) {
    if (key.endsWith(suffix)) {
      return key.substring(0, key.length() - suffix.length());
    }
    return key;
  }

  /**
   * Strips the prefix from the key if it is present. For example, for input key
   * ufs://my-bucket-name/my-key/file and prefix ufs://my-bucket-name/, the output would be
   * my-key/file. This method will leave keys without a prefix unaltered, ie. my-key/file
   * returns my-key/file.
   *
   * @param key the key to strip
   * @param prefix prefix to remove
   * @return the key without the prefix
   */
  public static String stripPrefixIfPresent(final String key, final String prefix) {
    if (key.startsWith(prefix)) {
      return key.substring(prefix.length());
    }
    return key;
  }

  /**
   * Strips the leading and trailing quotes from the given string.
   * E.g. return 'alluxio' for input '"alluxio"'.
   *
   * @param str The string to strip
   * @return The string without the leading and trailing quotes
   */
  public static String stripLeadingAndTrailingQuotes(String str) {
    int length = str.length();
    if (length > 1 && str.startsWith("\"") && str.endsWith("\"")) {
      str = str.substring(1, length - 1);
    }
    return str;
  }

  /**
   * Gets the value with a given key from a static key/value mapping in string format. E.g. with
   * mapping "id1=user1;id2=user2", it returns "user1" with key "id1". It returns null if the given
   * key does not exist in the mapping.
   *
   * @param mapping the "key=value" mapping in string format separated by ";"
   * @param key the key to query
   * @return the mapped value if the key exists, otherwise returns null
   */
  @Nullable
  public static String getValueFromStaticMapping(String mapping, String key) {
    Map<String, String> m = Splitter.on(";")
        .omitEmptyStrings()
        .trimResults()
        .withKeyValueSeparator("=")
        .split(mapping);
    return m.get(key);
  }

  /**
   * Gets the root cause of an exception.
   * It stops at encountering gRPC's StatusRuntimeException.
   *
   * @param e the exception
   * @return the root cause
   */
  public static Throwable getRootCause(Throwable e) {
    while (e.getCause() != null && !(e.getCause() instanceof StatusRuntimeException)) {
      e = e.getCause();
    }
    return e;
  }

  /**
   * Casts a {@link Throwable} to an {@link IOException}.
   *
   * @param e the throwable
   * @return the IO exception
   */
  public static IOException castToIOException(Throwable e) {
    if (e instanceof IOException) {
      return (IOException) e;
    } else {
      return new IOException(e);
    }
  }


  /**
   * Waits for a condition to be satisfied.
   *
   * @param description a description of what causes condition to evaluate to true
   * @param condition the condition to wait on
   * @throws TimeoutException if the function times out while waiting for the condition to be true
   */
  public static void waitFor(String description, Supplier<Boolean> condition)
      throws InterruptedException, TimeoutException {
    waitFor(description, condition, WaitForOptions.defaults());
  }

  /**
   * Waits for a condition to be satisfied.
   *
   * @param description a description of what causes condition to evaluate to true
   * @param condition the condition to wait on
   * @param options the options to use
   * @throws TimeoutException if the function times out while waiting for the condition to be true
   */
  public static void waitFor(String description, Supplier<Boolean> condition,
                             WaitForOptions options) throws InterruptedException, TimeoutException {
    waitForResult(description, condition, (b) -> b, options);
  }



  public static <T> T waitForResult(String description, Supplier<T> objectSupplier,
                                    Function<T, Boolean> condition, WaitForOptions options)
      throws TimeoutException, InterruptedException {
    T value;
    long start = getCurrentMs();
    int interval = options.getInterval();
    long timeout = options.getTimeoutMs();
    while (condition.apply(value = objectSupplier.get()) != true) {
      if (timeout != WaitForOptions.NEVER && getCurrentMs() - start > timeout) {
        throw new TimeoutException("Timed out waiting for " + description + " options: " + options
            + " last value: " + ObjectUtils.toString(value));
      }
      Thread.sleep(interval);
    }
    return value;
  }


  public static long getCurrentMs() {
    return Instant.now().toEpochMilli();
  }

  public static void sleepMs(long timeMs) {
    // TODO(adit): remove this wrapper
    SleepUtils.sleepMs(timeMs);
  }

  public static void sleepMs(Logger logger, long timeMs) {
    // TODO(adit): remove this wrapper
    SleepUtils.sleepMs(logger, timeMs);
  }

  /**
   * Partitions a list into numLists many lists each with around list.size() / numLists elements.
   *
   * @param list the list to partition
   * @param numLists number of lists to return
   * @param <T> the object type
   * @return partitioned list
   */
  public static <T> List<List<T>> partition(List<T> list, int numLists) {
    ArrayList<List<T>> result = new ArrayList<>(numLists);

    for (int i = 0; i < numLists; i++) {
      result.add(new ArrayList<>(list.size() / numLists + 1));
    }

    for (int i = 0; i < list.size(); i++) {
      result.get(i % numLists).add(list.get(i));
    }

    return result;
  }

  public static boolean isFatalError(Throwable e) {
    // StackOverflowError ok even though it is a VirtualMachineError
    if (e instanceof StackOverflowError) {
      return false;
    }
    // VirtualMachineError includes OutOfMemoryError and other fatal errors
    return e instanceof VirtualMachineError || e instanceof LinkageError;
  }
}
