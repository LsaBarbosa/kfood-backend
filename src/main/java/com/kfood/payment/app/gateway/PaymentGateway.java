package com.kfood.payment.app.gateway;

public interface PaymentGateway {

  String providerCode();

  CreatePixChargeGatewayResult createPixCharge(CreatePixChargeGatewayCommand command);
}
