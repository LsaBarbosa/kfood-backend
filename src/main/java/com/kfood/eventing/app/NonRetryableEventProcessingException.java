package com.kfood.eventing.app;

public class NonRetryableEventProcessingException extends RuntimeException {

  public NonRetryableEventProcessingException(String message) {
    super(message);
  }

  public NonRetryableEventProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
