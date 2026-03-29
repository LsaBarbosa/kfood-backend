package com.kfood.payment.app.gateway;

public class UnsupportedPaymentProviderException extends PaymentGatewayException {

  public UnsupportedPaymentProviderException(String providerCode) {
    super(
        providerCode,
        PaymentGatewayErrorType.PROVIDER_NOT_SUPPORTED,
        "Payment provider is not supported: " + providerCode);
  }
}
