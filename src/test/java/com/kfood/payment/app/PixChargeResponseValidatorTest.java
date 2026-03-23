package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.payment.app.gateway.CreatePixChargeGatewayResult;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PixChargeResponseValidatorTest {

  @Test
  void shouldAcceptValidPixChargeResponse() {
    assertThatCode(
            () ->
                PixChargeResponseValidator.validate(
                    "default-psp",
                    new CreatePixChargeGatewayResult(
                        "default-psp", "ref-1", "qr-1", OffsetDateTime.now().plusMinutes(15))))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectNullIncompleteAndExpiredPixChargeResponses() {
    assertUnavailable(
        () -> PixChargeResponseValidator.validate("default-psp", null),
        "Payment provider returned null Pix charge response. Provider: default-psp.");
    assertUnavailable(
        () ->
            PixChargeResponseValidator.validate(
                "default-psp",
                new CreatePixChargeGatewayResult(
                    "default-psp", " ", " ", OffsetDateTime.now().plusMinutes(15))),
        "Payment provider returned incomplete Pix charge response. Provider: default-psp.");
    assertUnavailable(
        () ->
            PixChargeResponseValidator.validate(
                "default-psp",
                new CreatePixChargeGatewayResult(
                    "default-psp", "ref-1", " ", OffsetDateTime.now().plusMinutes(15))),
        "Payment provider returned incomplete Pix charge response. Provider: default-psp.");
    assertUnavailable(
        () ->
            PixChargeResponseValidator.validate(
                "default-psp",
                new CreatePixChargeGatewayResult(
                    "default-psp", "ref-1", "qr-1", OffsetDateTime.now().minusMinutes(1))),
        "Payment provider returned expired Pix charge response. Provider: default-psp.");
  }

  @Test
  void shouldCoverBlankHelperWithBlankAndText() throws Exception {
    Method method = PixChargeResponseValidator.class.getDeclaredMethod("isBlank", String.class);
    method.setAccessible(true);

    assertThat((Boolean) method.invoke(null, " ")).isTrue();
    assertThat((Boolean) method.invoke(null, "value")).isFalse();
  }

  private void assertUnavailable(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String message) {
    assertThatThrownBy(callable)
        .isInstanceOf(BusinessException.class)
        .satisfies(
            exception -> {
              var businessException = (BusinessException) exception;
              assertThat(businessException.getErrorCode())
                  .isEqualTo(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
              assertThat(businessException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
              assertThat(businessException.getMessage()).isEqualTo(message);
            });
  }
}
