package com.bollu.goosefs.common.exception;

public class AccessControlException extends GooseFSException{
  private static final long serialVersionUID = 7147533331335543932L;

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public AccessControlException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public AccessControlException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the specified exception message and multiple parameters.
   *
   * @param message the exception message
   * @param params the parameters
   */
  public AccessControlException(ExceptionMessage message, Object... params) {
    this(message.getMessage(params));
  }

  /**
   * Constructs a new exception with the specified exception message, the cause and multiple
   * parameters.
   *
   * @param message the exception message
   * @param cause the cause
   * @param params the parameters
   */
  public AccessControlException(ExceptionMessage message, Throwable cause, Object... params) {
    this(message.getMessage(params), cause);
  }
}
