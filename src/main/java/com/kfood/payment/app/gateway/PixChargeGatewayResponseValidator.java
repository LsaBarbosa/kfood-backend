package com.kfood.payment.app.gateway;

import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class PixChargeGatewayResponseValidator {

  private final Clock clock;

  public PixChargeGatewayResponseValidator() {
    this(Clock.systemUTC());
  }

  PixChargeGatewayResponseValidator(Clock clock) {
    this.clock = clock;
  }

  public void ensureValid(String providerCode, CreatePixChargeResponse response) {
    if (response == null) {
      throw invalidResponse(providerCode, "Pix charge response is required.");
    }
    if (response.providerReference() == null || response.providerReference().isBlank()) {
      throw invalidResponse(providerCode, "Pix charge response must include provider reference.");
    }
    if (response.qrCodePayload() == null || response.qrCodePayload().isBlank()) {
      throw invalidResponse(providerCode, "Pix charge response must include qr code payload.");
    }
    if (response.expiresAt() == null) {
      throw invalidResponse(providerCode, "Pix charge response must include expiration.");
    }
    if (!response.expiresAt().isAfter(clock.instant().atOffset(response.expiresAt().getOffset()))) {
      throw invalidResponse(providerCode, "Pix charge response expiration must be in the future.");
    }
  }

  private static PaymentGatewayException invalidResponse(String providerCode, String message) {
    return new PaymentGatewayException(
        providerCode, PaymentGatewayErrorType.INVALID_REQUEST, message);
  }
}
