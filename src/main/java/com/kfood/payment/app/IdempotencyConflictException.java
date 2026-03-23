package com.kfood.payment.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends BusinessException {

  public IdempotencyConflictException(String message) {
    super(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD, message, HttpStatus.CONFLICT);
  }
}
