package com.bollu.goosefs.common.exception;

public class GooseFSException extends Exception{
  private static final long serialVersionUID = 2243833925609642384L;

  /**
   * Constructs an {@link GooseFSException} with the given cause.
   *
   * @param cause the cause
   */
  protected GooseFSException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs an {@link GooseFSException} with the given message.
   *
   * @param message the message
   */
  public GooseFSException(String message) {
    super(message);
  }

  /**
   * Constructs an {@link GooseFSException} with the given message and cause.
   *
   * @param message the message
   * @param cause the cause
   */
  public GooseFSException(String message, Throwable cause) {
    super(message, cause);
  }
}
