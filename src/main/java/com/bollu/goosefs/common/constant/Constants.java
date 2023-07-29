package com.bollu.goosefs.common.constant;

public class Constants {

  public static final String SITE_PROPERTIES = "goosefs-site.properties";

  public static final int KB = 1024;
  public static final int MB = KB * 1024;
  public static final int GB = MB * 1024;
  public static final long TB = GB * 1024L;
  public static final long PB = TB * 1024L;

  public static final long SECOND = 1000;
  public static final long MINUTE = SECOND * 60L;
  public static final long HOUR = MINUTE * 60L;
  public static final long DAY = HOUR * 24L;

  public static final int MS_NANO = 1_000_000;
  public static final long SECOND_NANO = 1_000_000_000L;
  public static final int SECOND_MS = 1_000;
  public static final int MINUTE_MS = SECOND_MS * 60;
  public static final int HOUR_MS = MINUTE_MS * 60;
  public static final int DAY_MS = HOUR_MS * 24;
  public static final int MINUTE_SECONDS = 60;

  public static final int DEFAULT_REGISTRY_GET_TIMEOUT_MS = 60 * SECOND_MS;
}
