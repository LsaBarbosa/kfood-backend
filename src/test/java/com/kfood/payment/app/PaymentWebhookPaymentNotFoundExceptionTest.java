package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.shared.exceptions.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PaymentWebhookPaymentNotFoundExceptionTest {

  @Test
  void shouldBuildSpecificMessageWhenProviderAndReferenceArePresent() {
    var exception = new PaymentWebhookPaymentNotFoundException("mock-psp", "ref-1");

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(exception.getMessage())
        .isEqualTo("Payment not found for provider mock-psp and reference ref-1.");
  }

  @Test
  void shouldBuildGenericMessageWhenProviderOrReferenceAreBlank() {
    assertThat(new PaymentWebhookPaymentNotFoundException(null, "ref-1").getMessage())
        .isEqualTo("Payment not found for the informed provider and reference.");
    assertThat(new PaymentWebhookPaymentNotFoundException(" ", "ref-1").getMessage())
        .isEqualTo("Payment not found for the informed provider and reference.");
    assertThat(new PaymentWebhookPaymentNotFoundException("mock-psp", null).getMessage())
        .isEqualTo("Payment not found for the informed provider and reference.");
    assertThat(new PaymentWebhookPaymentNotFoundException("mock-psp", " ").getMessage())
        .isEqualTo("Payment not found for the informed provider and reference.");
  }
}
