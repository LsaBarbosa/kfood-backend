package com.kfood.payment.app.gateway;

public class PaymentGatewayException extends RuntimeException {

  private final String provider;
  private final PaymentGatewayErrorType errorType;

  public PaymentGatewayException(
      String provider, PaymentGatewayErrorType errorType, String message, Throwable cause) {
    super(message, cause);
    this.provider = provider;
    this.errorType = errorType;
  }

  public PaymentGatewayException(
      String provider, PaymentGatewayErrorType errorType, String message) {
    this(provider, errorType, message, null);
  }

  public static PaymentGatewayException timeout(String provider, String message, Throwable cause) {
    return new PaymentGatewayException(provider, PaymentGatewayErrorType.TIMEOUT, message, cause);
  }

  public static PaymentGatewayException unavailable(
      String provider, String message, Throwable cause) {
    return new PaymentGatewayException(
        provider, PaymentGatewayErrorType.UNAVAILABLE, message, cause);
  }

  public static PaymentGatewayException authentication(
      String provider, String message, Throwable cause) {
    return new PaymentGatewayException(
        provider, PaymentGatewayErrorType.AUTHENTICATION, message, cause);
  }

  public static PaymentGatewayException badRequest(
      String provider, String message, Throwable cause) {
    return new PaymentGatewayException(
        provider, PaymentGatewayErrorType.BAD_REQUEST, message, cause);
  }

  public static PaymentGatewayException invalidResponse(
      String provider, String message, Throwable cause) {
    return new PaymentGatewayException(
        provider, PaymentGatewayErrorType.INVALID_RESPONSE, message, cause);
  }

  public String getProvider() {
    return provider;
  }

  public PaymentGatewayErrorType getErrorType() {
    return errorType;
  }
}
