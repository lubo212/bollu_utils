package com.bollu.utils.goosefs.common.utils;

import com.bollu.utils.goosefs.common.constant.Constants;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatUtils {

  private static final Pattern SEP_DIGIT_LETTER = Pattern.compile("([-]?[0-9]*)([a-zA-Z]*)");
  public static long parseSpaceSize(String spaceSize) {
    double alpha = 0.0001;
    String ori = spaceSize;
    String end = "";
    int index = spaceSize.length() - 1;
    while (index >= 0) {
      if (spaceSize.charAt(index) > '9' || spaceSize.charAt(index) < '0') {
        end = spaceSize.charAt(index) + end;
      } else {
        break;
      }
      index--;
    }
    spaceSize = spaceSize.substring(0, index + 1);
    double ret = Double.parseDouble(spaceSize);
    end = end.toLowerCase();
    if (end.isEmpty() || end.equals("b")) {
      return (long) (ret + alpha);
    } else if (end.equals("kb") || end.equals("k")) {
      return (long) (ret * Constants.KB + alpha);
    } else if (end.equals("mb") || end.equals("m")) {
      return (long) (ret * Constants.MB + alpha);
    } else if (end.equals("gb") || end.equals("g")) {
      return (long) (ret * Constants.GB + alpha);
    } else if (end.equals("tb") || end.equals("t")) {
      return (long) (ret * Constants.TB + alpha);
    } else if (end.equals("pb") || end.equals("p")) {
      // When parsing petabyte values, we can't multiply with doubles and longs, since that will
      // lose presicion with such high numbers. Therefore we use a BigDecimal.
      BigDecimal pBDecimal = new BigDecimal(Constants.PB);
      return pBDecimal.multiply(BigDecimal.valueOf(ret)).longValue();
    } else {
      throw new IllegalArgumentException("Fail to parse " + ori + " to bytes");
    }
  }

  public static long parseTimeSize(String timeSize) {
    double alpha = 0.0001;
    String time = "";
    String size = "";
    Matcher m = SEP_DIGIT_LETTER.matcher(timeSize);
    if (m.matches()) {
      time = m.group(1);
      size = m.group(2);
    }
    double douTime = Double.parseDouble(time);
    long sign = 1;
    if (douTime < 0) {
      sign = -1;
      douTime = -douTime;
    }
    size = size.toLowerCase();
    if (size.isEmpty() || size.equalsIgnoreCase("ms")
        || size.equalsIgnoreCase("millisecond")) {
      return sign * (long) (douTime + alpha);
    } else if (size.equalsIgnoreCase("s") || size.equalsIgnoreCase("sec")
        || size.equalsIgnoreCase("second")) {
      return sign * (long) (douTime * Constants.SECOND + alpha);
    } else if (size.equalsIgnoreCase("m") || size.equalsIgnoreCase("min")
        || size.equalsIgnoreCase("minute")) {
      return sign * (long) (douTime * Constants.MINUTE + alpha);
    } else if (size.equalsIgnoreCase("h") || size.equalsIgnoreCase("hr")
        || size.equalsIgnoreCase("hour")) {
      return sign * (long) (douTime * Constants.HOUR + alpha);
    } else if (size.equalsIgnoreCase("d") || size.equalsIgnoreCase("day")) {
      return sign * (long) (douTime * Constants.DAY + alpha);
    } else {
      throw new IllegalArgumentException("Fail to parse " + timeSize + " to milliseconds");
    }
  }
}
