package com.kfood.payment.app.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentGatewayExceptionTest {

  @Test
  void shouldCreateGatewayExceptionsForAllErrorTypes() {
    var cause = new IllegalStateException("boom");

    assertException(
        PaymentGatewayException.timeout("mock-psp", "timeout", cause),
        PaymentGatewayErrorType.TIMEOUT,
        "mock-psp",
        "timeout",
        cause);
    assertException(
        PaymentGatewayException.unavailable("mock-psp", "unavailable", cause),
        PaymentGatewayErrorType.UNAVAILABLE,
        "mock-psp",
        "unavailable",
        cause);
    assertException(
        PaymentGatewayException.authentication("mock-psp", "auth", cause),
        PaymentGatewayErrorType.AUTHENTICATION,
        "mock-psp",
        "auth",
        cause);
    assertException(
        PaymentGatewayException.badRequest("mock-psp", "bad-request", cause),
        PaymentGatewayErrorType.BAD_REQUEST,
        "mock-psp",
        "bad-request",
        cause);
    assertException(
        PaymentGatewayException.invalidResponse("mock-psp", "invalid-response", cause),
        PaymentGatewayErrorType.INVALID_RESPONSE,
        "mock-psp",
        "invalid-response",
        cause);
  }

  @Test
  void shouldSupportConstructorWithoutCause() {
    var exception =
        new PaymentGatewayException("mock-psp", PaymentGatewayErrorType.UNAVAILABLE, "message");

    assertThat(exception.getProvider()).isEqualTo("mock-psp");
    assertThat(exception.getErrorType()).isEqualTo(PaymentGatewayErrorType.UNAVAILABLE);
    assertThat(exception.getMessage()).isEqualTo("message");
    assertThat(exception.getCause()).isNull();
  }

  private void assertException(
      PaymentGatewayException exception,
      PaymentGatewayErrorType errorType,
      String provider,
      String message,
      Throwable cause) {
    assertThat(exception.getErrorType()).isEqualTo(errorType);
    assertThat(exception.getProvider()).isEqualTo(provider);
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
  }
}
