package com.kfood.payment.app.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.shared.exceptions.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PaymentGatewayExceptionTest {

  @Test
  void shouldExposeControlledGatewayExceptionContract() {
    var cause = new IllegalStateException("timeout");
    var exception =
        new PaymentGatewayException(
            "mock", PaymentGatewayErrorType.TIMEOUT, "Provider timed out", cause);

    assertThat(exception.getProviderCode()).isEqualTo("mock");
    assertThat(exception.getErrorType()).isEqualTo(PaymentGatewayErrorType.TIMEOUT);
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(exception).hasMessage("Provider timed out");
    assertThat(exception.getCause()).isSameAs(cause);
  }

  @Test
  void shouldExposeUnsupportedProviderExceptionContract() {
    var exception = new UnsupportedPaymentProviderException("unknown");

    assertThat(exception.getProviderCode()).isEqualTo("unknown");
    assertThat(exception.getErrorType()).isEqualTo(PaymentGatewayErrorType.PROVIDER_NOT_SUPPORTED);
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(exception).hasMessage("Payment provider is not supported: unknown");
  }
}
