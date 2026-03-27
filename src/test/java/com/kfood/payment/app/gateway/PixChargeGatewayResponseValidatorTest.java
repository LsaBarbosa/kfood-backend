package com.kfood.payment.app.gateway;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PixChargeGatewayResponseValidatorTest {

  private final PixChargeGatewayResponseValidator validator =
      new PixChargeGatewayResponseValidator(
          Clock.fixed(Instant.parse("2026-03-27T15:00:00Z"), ZoneOffset.UTC));

  @Test
  void shouldRejectNullResponse() {
    assertInvalid(null, "Pix charge response is required.");
  }

  @Test
  void shouldRejectBlankProviderReference() {
    assertInvalid(
        new CreatePixChargeResponse("mock", "   ", "pix:payload", futureExpiration()),
        "Pix charge response must include provider reference.");
  }

  @Test
  void shouldRejectBlankQrCodePayload() {
    assertInvalid(
        new CreatePixChargeResponse("mock", "tx-123", "  ", futureExpiration()),
        "Pix charge response must include qr code payload.");
  }

  @Test
  void shouldRejectNullExpiration() {
    assertInvalid(
        new CreatePixChargeResponse("mock", "tx-123", "pix:payload", null),
        "Pix charge response must include expiration.");
  }

  @Test
  void shouldRejectExpiredResponse() {
    assertInvalid(
        new CreatePixChargeResponse(
            "mock", "tx-123", "pix:payload", OffsetDateTime.parse("2026-03-27T14:59:59Z")),
        "Pix charge response expiration must be in the future.");
  }

  @Test
  void shouldAcceptValidResponse() {
    assertThatCode(
            () ->
                validator.ensureValid(
                    "mock",
                    new CreatePixChargeResponse(
                        "mock", "tx-123", "pix:payload", futureExpiration())))
        .doesNotThrowAnyException();
  }

  private void assertInvalid(CreatePixChargeResponse response, String message) {
    assertThatThrownBy(() -> validator.ensureValid("mock", response))
        .isInstanceOf(PaymentGatewayException.class)
        .satisfies(
            throwable -> {
              var ex = (PaymentGatewayException) throwable;
              assertThat(ex.getProviderCode()).isEqualTo("mock");
              assertThat(ex.getErrorType()).isEqualTo(PaymentGatewayErrorType.INVALID_REQUEST);
            })
        .hasMessage(message);
  }

  private static OffsetDateTime futureExpiration() {
    return OffsetDateTime.parse("2026-03-27T15:30:00Z");
  }
}
