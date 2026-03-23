package com.kfood.payment.app;

import com.kfood.payment.app.gateway.CreatePixChargeGatewayResult;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;

public final class PixChargeResponseValidator {

  private PixChargeResponseValidator() {}

  public static void validate(String provider, CreatePixChargeGatewayResult result) {
    if (result == null) {
      throw unavailable(provider, "Payment provider returned null Pix charge response.");
    }

    if (isBlank(result.providerReference()) || isBlank(result.qrCodePayload())) {
      throw unavailable(provider, "Payment provider returned incomplete Pix charge response.");
    }

    if (!result.expiresAt().isAfter(OffsetDateTime.now())) {
      throw unavailable(provider, "Payment provider returned expired Pix charge response.");
    }
  }

  private static boolean isBlank(String value) {
    return value.isBlank();
  }

  private static BusinessException unavailable(String provider, String message) {
    return new BusinessException(
        ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE,
        message + " Provider: " + provider + ".",
        HttpStatus.SERVICE_UNAVAILABLE);
  }
}
