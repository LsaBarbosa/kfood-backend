package com.kfood.payment.app.gateway;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class PaymentGatewayException extends BusinessException {

  private final String providerCode;
  private final PaymentGatewayErrorType errorType;

  public PaymentGatewayException(
      String providerCode, PaymentGatewayErrorType errorType, String message) {
    super(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE, message, HttpStatus.BAD_GATEWAY);
    this.providerCode = providerCode;
    this.errorType = errorType;
  }

  public PaymentGatewayException(
      String providerCode, PaymentGatewayErrorType errorType, String message, Throwable cause) {
    this(providerCode, errorType, message);
    initCause(cause);
  }

  public String getProviderCode() {
    return providerCode;
  }

  public PaymentGatewayErrorType getErrorType() {
    return errorType;
  }
}
