package com.kfood.eventing.app;

public class RetryableEventProcessingException extends RuntimeException {

  public RetryableEventProcessingException(String message) {
    super(message);
  }

  public RetryableEventProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
