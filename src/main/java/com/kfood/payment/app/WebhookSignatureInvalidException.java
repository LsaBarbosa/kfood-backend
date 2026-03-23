package com.kfood.payment.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class WebhookSignatureInvalidException extends BusinessException {

  public WebhookSignatureInvalidException(String message) {
    super(ErrorCode.WEBHOOK_SIGNATURE_INVALID, message, HttpStatus.UNAUTHORIZED);
  }
}
