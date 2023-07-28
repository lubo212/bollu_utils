package com.bollu.goosefs.config;

import com.google.common.base.Objects;

public class Source implements Comparable<Source> {

  public static final Source UNKNOWN = new Source(Type.UNKNOWN);
  public static final Source DEFAULT = new Source(Type.DEFAULT);
  public static final Source CLUSTER_DEFAULT = new Source(Type.CLUSTER_DEFAULT);
  public static final Source SYSTEM_PROPERTY = new Source(Type.SYSTEM_PROPERTY);
  public static final Source PATH_DEFAULT = new Source(Type.PATH_DEFAULT);
  public static final Source RUNTIME = new Source(Type.RUNTIME);
  public static final Source MOUNT_OPTION = new Source(Type.MOUNT_OPTION);

  public enum Type {
    /**
     * The unknown source which has the lowest priority.
     */
    UNKNOWN,
    /**
     * The default property value from <code>PropertyKey</code> on compile time.
     */
    DEFAULT,
    /**
     * The default property value as loaded from the masters of the cluster.
     */
    CLUSTER_DEFAULT,
    /**
     * The property value is specified in site properties file (goosefs-site.properties).
     */
    SITE_PROPERTY,
    /**
     * The property value is specified with JVM -D options before passed to GooseFS.
     */
    SYSTEM_PROPERTY,
    /**
     * The property value is specified as path level defaults through command line.
     */
    PATH_DEFAULT,
    /**
     * The property value is set by user during runtime (e.g., Configuration.set or through
     * HadoopConf).
     */
    RUNTIME,
    /**
     * The property value is specified as options for a mount point. This source has the highest
     * priority.
     */
    MOUNT_OPTION,
  }

  protected final Type mType;

  private Source(Type type) {
    mType = type;
  }

  public Type getType() {
    return mType;
  }

  public static Source siteProperty(String filename) {
    return new SitePropertySource(filename);
  }

  @Override
  public int compareTo(Source other) {
    return mType.compareTo(other.mType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Source)) {
      return false;
    }

    Source other = (Source) o;

    return compareTo(other) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mType);
  }

  @Override
  public String toString() {
    return mType.name();
  }

  private static final class SitePropertySource extends Source {
    private final String mFilename;

    private SitePropertySource(String filename) {
      super(Type.SITE_PROPERTY);
      mFilename = filename;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }
      SitePropertySource that = (SitePropertySource) o;
      return Objects.equal(mFilename, that.mFilename);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(mFilename);
    }

    @Override
    public String toString() {
      return String.format("%s (%s)", mType, mFilename);
    }
  }
}
