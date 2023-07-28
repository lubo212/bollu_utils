package com.bollu.goosefs.config.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

public class PathUtils {

  private static final CharMatcher SEPARATOR_MATCHER =
      CharMatcher.is('/');

  public static String concatPath(Object base, Object path) {
    Preconditions.checkNotNull(base, "base");
    Preconditions.checkNotNull(path, "path");
    String trimmedBase = SEPARATOR_MATCHER.trimTrailingFrom(base.toString());
    String trimmedPath = SEPARATOR_MATCHER.trimFrom(path.toString());

    StringBuilder output = new StringBuilder(trimmedBase.length() + trimmedPath.length() + 1);
    output.append(trimmedBase);
    if (!trimmedPath.isEmpty()) {
      output.append(SEPARATOR_MATCHER);
      output.append(trimmedPath);
    }

    if (output.length() == 0) {
      // base must be "[/]+"
      return String.valueOf(SEPARATOR_MATCHER);
    }
    return output.toString();
  }
}
