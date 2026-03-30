package com.kfood.payment.app.gateway;

public interface PixChargeGateway {

  String providerCode();

  CreatePixChargeResponse createCharge(CreatePixChargeRequest request);
}
