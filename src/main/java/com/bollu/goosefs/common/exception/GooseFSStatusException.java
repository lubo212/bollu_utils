package com.bollu.goosefs.common.exception;

import com.bollu.goosefs.common.exception.status.UnavailableException;
import com.bollu.goosefs.common.exception.status.UnknownException;
import com.google.common.base.Preconditions;
import io.grpc.Status;
import io.grpc.StatusException;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

public class GooseFSStatusException extends IOException {
  private static final long serialVersionUID = -7422144873058169662L;

  private final Status mStatus;

  public GooseFSStatusException(Status status) {
    super(status.getDescription(), status.getCause());
    mStatus = status;
  }

  public Status getStatus() {
    return mStatus;
  }

  public Status.Code getStatusCode() {
    return mStatus.getCode();
  }

  public GooseFSException toGooseFSException() {
    switch (mStatus.getCode()) {
      // Fall throughs are intentional.
      case PERMISSION_DENIED:
      case UNAUTHENTICATED:
        return new AccessControlException(getMessage(), this);
      case ABORTED:
      case ALREADY_EXISTS:
      case CANCELLED:
      case DATA_LOSS:
      case DEADLINE_EXCEEDED:
      case FAILED_PRECONDITION:
      case INTERNAL:
      case INVALID_ARGUMENT:
      case NOT_FOUND:
      case OUT_OF_RANGE:
      case RESOURCE_EXHAUSTED:
      case UNAVAILABLE:
      case UNIMPLEMENTED:
      case UNKNOWN:
      default:
        return new GooseFSException(getMessage(), this);
    }
  }

  public StatusException toGrpcStatusException() {
    return mStatus.asException();
  }

  public static GooseFSStatusException from(Status status) {
    Preconditions.checkNotNull(status, "status");
    Preconditions.checkArgument(status != Status.OK, "OK is not an error status");
    final String message = status.getDescription();
    final Throwable cause = status.getCause();
    switch (status.getCode()) {
      default:
        Throwable nestedCause = cause;
        while (nestedCause != null) {
          if (nestedCause instanceof ClosedChannelException) {
            // GRPC can mask closed channels as unknown exceptions, but unavailable is more
            // appropriate
            return new UnavailableException(message, cause);
          }
          nestedCause = nestedCause.getCause();
        }
        return new UnknownException(message, cause);
    }
  }

}
