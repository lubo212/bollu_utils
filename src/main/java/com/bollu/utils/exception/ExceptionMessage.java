package com.bollu.utils.exception;

import com.google.common.base.Preconditions;

import java.text.MessageFormat;

public enum ExceptionMessage {

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
