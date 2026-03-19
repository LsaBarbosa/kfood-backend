package com.kfood.shared.exceptions;

import java.util.List;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final HttpStatus status;
  private final List<ApiFieldError> details;

  public BusinessException(ErrorCode errorCode, String message, HttpStatus status) {
    this(errorCode, message, status, List.of());
  }

  public BusinessException(
      ErrorCode errorCode, String message, HttpStatus status, List<ApiFieldError> details) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
    this.details = details == null ? List.of() : List.copyOf(details);
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public List<ApiFieldError> getDetails() {
    return details;
  }
}
