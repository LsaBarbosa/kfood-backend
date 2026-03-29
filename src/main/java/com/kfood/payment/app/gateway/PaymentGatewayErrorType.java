package com.kfood.payment.app.gateway;

public enum PaymentGatewayErrorType {
  INVALID_REQUEST,
  PROVIDER_NOT_SUPPORTED,
  PROVIDER_UNAVAILABLE,
  TIMEOUT,
  UNEXPECTED_ERROR
}
