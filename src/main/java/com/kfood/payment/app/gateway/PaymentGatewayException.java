package com.kfood.payment.app.gateway;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class PaymentGatewayException extends BusinessException {

  private final String providerCode;
  private final PaymentGatewayErrorType errorType;

  public PaymentGatewayException(
      String providerCode, PaymentGatewayErrorType errorType, String message) {
    super(mapErrorCode(errorType), message, mapStatus(errorType));
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

  private static ErrorCode mapErrorCode(PaymentGatewayErrorType errorType) {
    return switch (errorType) {
      case INVALID_REQUEST -> ErrorCode.PAYMENT_PROVIDER_INVALID_RESPONSE;
      case PROVIDER_NOT_SUPPORTED -> ErrorCode.PAYMENT_PROVIDER_NOT_SUPPORTED;
      case PROVIDER_UNAVAILABLE, UNEXPECTED_ERROR -> ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE;
      case TIMEOUT -> ErrorCode.PAYMENT_PROVIDER_TIMEOUT;
    };
  }

  private static HttpStatus mapStatus(PaymentGatewayErrorType errorType) {
    return switch (errorType) {
      case INVALID_REQUEST, PROVIDER_UNAVAILABLE, UNEXPECTED_ERROR -> HttpStatus.BAD_GATEWAY;
      case PROVIDER_NOT_SUPPORTED -> HttpStatus.BAD_REQUEST;
      case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
    };
  }
}
