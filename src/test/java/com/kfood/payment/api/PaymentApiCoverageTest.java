package com.kfood.payment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.payment.app.CreatePixPaymentUseCase;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.WebhookProcessingStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PaymentApiCoverageTest {

  @Test
  void shouldInstantiatePaymentApiRecords() {
    var paymentId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    var expiresAt = OffsetDateTime.parse("2026-03-22T12:15:00Z");

    var createPixPaymentRequest =
        new CreatePixPaymentRequest(new BigDecimal("56.50"), "default-psp");
    var createPixPaymentResponse =
        new CreatePixPaymentResponse(
            paymentId,
            orderId,
            PaymentMethod.PIX,
            PaymentStatus.PENDING,
            "psp_100",
            "0002012636mockpix100",
            expiresAt);
    var paymentWebhookRequest =
        new PaymentWebhookRequest(
            "evt_123",
            "PAYMENT_CONFIRMED",
            "psp_123",
            "2026-03-22T12:00:00Z",
            new BigDecimal("10.00"));
    var paymentWebhookResponse =
        new PaymentWebhookResponse(true, WebhookProcessingStatus.PROCESSED, "evt_123");

    assertThat(createPixPaymentRequest.amount()).isEqualByComparingTo("56.50");
    assertThat(createPixPaymentRequest.provider()).isEqualTo("default-psp");
    assertThat(createPixPaymentResponse.paymentId()).isEqualTo(paymentId);
    assertThat(createPixPaymentResponse.orderId()).isEqualTo(orderId);
    assertThat(createPixPaymentResponse.paymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(createPixPaymentResponse.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(createPixPaymentResponse.providerReference()).isEqualTo("psp_100");
    assertThat(createPixPaymentResponse.qrCodePayload()).isEqualTo("0002012636mockpix100");
    assertThat(createPixPaymentResponse.expiresAt()).isEqualTo(expiresAt);
    assertThat(paymentWebhookRequest.hasAtLeastOneIdempotencyIdentifier()).isTrue();
    assertThat(paymentWebhookResponse.accepted()).isTrue();
    assertThat(paymentWebhookResponse.processingStatus())
        .isEqualTo(WebhookProcessingStatus.PROCESSED);
    assertThat(paymentWebhookResponse.externalEventId()).isEqualTo("evt_123");
  }

  @Test
  void shouldReportMissingWebhookIdempotencyIdentifiers() {
    var paymentWebhookRequest =
        new PaymentWebhookRequest(null, "PAYMENT_CONFIRMED", " ", null, null);

    assertThat(paymentWebhookRequest.hasAtLeastOneIdempotencyIdentifier()).isFalse();
  }

  @Test
  void shouldThrowWhenPaymentUseCaseIsUnavailable() {
    ObjectProvider<CreatePixPaymentUseCase> provider = new FixedObjectProvider<>(null);

    var controller = new PaymentController(provider);

    assertThatThrownBy(
            () ->
                controller.createPixPayment(
                    UUID.randomUUID(),
                    "idem-1",
                    new CreatePixPaymentRequest(new BigDecimal("10.00"), "default-psp")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("CreatePixPaymentUseCase is not available.");
  }

  @Test
  void shouldDelegateToAvailablePaymentUseCase() {
    var useCase = mock(CreatePixPaymentUseCase.class);
    ObjectProvider<CreatePixPaymentUseCase> provider = new FixedObjectProvider<>(useCase);
    var response =
        new CreatePixPaymentResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            PaymentMethod.PIX,
            PaymentStatus.PENDING,
            "psp_1",
            "qr",
            OffsetDateTime.parse("2026-03-22T12:15:00Z"));
    when(useCase.execute(org.mockito.ArgumentMatchers.any())).thenReturn(response);

    var controller = new PaymentController(provider);

    assertThat(
            controller.createPixPayment(
                UUID.randomUUID(),
                "idem-1",
                new CreatePixPaymentRequest(new BigDecimal("10.00"), "default-psp")))
        .isEqualTo(response);
  }

  private static final class FixedObjectProvider<T> implements ObjectProvider<T> {

    private final T value;

    private FixedObjectProvider(T value) {
      this.value = value;
    }

    @Override
    public T getObject(Object... args) {
      return value;
    }

    @Override
    public T getIfAvailable() {
      return value;
    }

    @Override
    public T getIfUnique() {
      return value;
    }

    @Override
    public T getObject() {
      return value;
    }
  }
}
