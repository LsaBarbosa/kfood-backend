package com.kfood.payment.app.gateway;

import java.time.OffsetDateTime;
import java.util.Objects;

public record CreatePixChargeGatewayResult(
    String providerName, String providerReference, String qrCodePayload, OffsetDateTime expiresAt) {

  public CreatePixChargeGatewayResult {
    Objects.requireNonNull(providerName, "providerName must not be null");
    Objects.requireNonNull(providerReference, "providerReference must not be null");
    Objects.requireNonNull(qrCodePayload, "qrCodePayload must not be null");
    Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }
}
