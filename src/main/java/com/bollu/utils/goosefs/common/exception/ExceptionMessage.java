package com.bollu.utils.goosefs.common.exception;

import com.google.common.base.Preconditions;

import java.text.MessageFormat;

public enum ExceptionMessage {

  INVALID_CONFIGURATION_KEY("Invalid property key {0}"),
  UNDEFINED_CONFIGURATION_KEY("No value set for configuration key {0}"),
  KEY_NOT_BOOLEAN("Configuration cannot evaluate value {0} as boolean for key {1}"),
  KEY_NOT_BYTES("Configuration cannot evaluate value {0} as bytes for key {1}"),
  KEY_NOT_DOUBLE("Configuration cannot evaluate value {0} as double for key {1}"),
  KEY_NOT_FLOAT("Configuration cannot evaluate value {0} as float for key {1}"),
  KEY_NOT_INTEGER("Configuration cannot evaluate value {0} as integer for key {1}"),
  KEY_NOT_LONG("Configuration cannot evaluate value {0} as long for key {1}"),
  KEY_NOT_MS("Configuration cannot evaluate value {0} as milliseconds for key {1}"),
  KEY_CIRCULAR_DEPENDENCY("Circular dependency found while resolving {0}"),
  UNKNOWN_ENUM("Unrecognized configuration enum value <{0}> for key {1}. Acceptable values: {2}"),
  INVALID_METRIC_KEY("Invalid metric key {0}");

  private final MessageFormat mMessage;

  ExceptionMessage(String message) {
    mMessage = new MessageFormat(message);
  }

  /**
   * Formats the message of the exception.
   *
   * @param params the parameters for the exception message
   * @return the formatted message
   */
  public String getMessage(Object... params) {
    Preconditions.checkArgument(mMessage.getFormatsByArgumentIndex().length == params.length,
        "The message takes " + mMessage.getFormatsByArgumentIndex().length + " arguments, but is "
            + "given " + params.length);
    // MessageFormat is not thread-safe, so guard it
    synchronized (mMessage) {
      return mMessage.format(params);
    }
  }

  /**
   * Formats the message of the exception with a url to consult.
   *
   * @param url the url to consult
   * @param params the parameters for the exception message
   * @return the formatted message
   */
  public String getMessageWithUrl(String url, Object... params) {
    return getMessage(params) + " Please consult " + url
        + " for common solutions to address this problem.";
  }
}
