/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package com.bollu.goosefs.common.exception.status;

import com.bollu.goosefs.common.exception.GooseFSStatusException;
import io.grpc.Status;

/**
 * Exception indicating that the request does not have valid authentication credentials for the
 * operation.
 */
public class UnauthenticatedException extends GooseFSStatusException {
  private static final long serialVersionUID = -8042434313008986493L;
  private static final Status STATUS = Status.UNAUTHENTICATED;

  /**
   * @param message the exception message
   */
  public UnauthenticatedException(String message) {
    super(STATUS.withDescription(message));
  }

  /**
   * @param cause the cause of the exception
   */
  public UnauthenticatedException(Throwable cause) {
    super(STATUS.withDescription(cause.getMessage()).withCause(cause));
  }

  /**
   * @param message the exception message
   * @param cause the cause of the exception
   */
  public UnauthenticatedException(String message, Throwable cause) {
    super(STATUS.withDescription(message).withCause(cause));
  }
}
