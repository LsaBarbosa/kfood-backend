package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfirmPaymentWebhookProcessorTest {

  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final ConfirmPaymentWebhookProcessor processor =
      new ConfirmPaymentWebhookProcessor(paymentRepository);

  @Test
  void shouldConfirmPaymentAndUpdateOrderSnapshot() {
    var order = order();
    var payment =
        Payment.create(
            UUID.randomUUID(), order, PaymentMethod.PIX, "mock-psp", "psp_ref_123", "payload");
    var event =
        PaymentWebhookEvent.received(
            null, "mock-psp", "evt_001", "evt_001", "{\"eventType\":\"PAYMENT_CONFIRMED\"}");
    var request =
        new PaymentWebhookRequest(
            "evt_001", "PAYMENT_CONFIRMED", "psp_ref_123", "2026-03-16T18:50:00Z", null);

    when(paymentRepository.findByProviderNameAndProviderReference("mock-psp", "psp_ref_123"))
        .thenReturn(Optional.of(payment));

    processor.process(event, request);

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(payment.getConfirmedAt()).isEqualTo(OffsetDateTime.parse("2026-03-16T18:50:00Z"));
    assertThat(order.getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PAID);
    assertThat(event.getPayment()).isEqualTo(payment);
    verify(paymentRepository).save(payment);
  }

  @Test
  void shouldThrowNotFoundWhenPaymentDoesNotExist() {
    var event =
        PaymentWebhookEvent.received(
            null, "mock-psp", "evt_404", "evt_404", "{\"eventType\":\"PAYMENT_CONFIRMED\"}");
    var request =
        new PaymentWebhookRequest(
            "evt_404", "PAYMENT_CONFIRMED", "psp_ref_404", "2026-03-16T18:50:00Z", null);

    when(paymentRepository.findByProviderNameAndProviderReference("mock-psp", "psp_ref_404"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> processor.process(event, request))
        .isInstanceOf(PaymentWebhookPaymentNotFoundException.class)
        .hasMessage("Payment not found for provider mock-psp and reference psp_ref_404.");
  }

  @Test
  void shouldThrowConflictWhenPaymentStatusIsFinalFailureState() {
    var order = order();
    var payment =
        Payment.create(
            UUID.randomUUID(), order, PaymentMethod.PIX, "mock-psp", "psp_ref_conflict", "payload");
    payment.markFailed();
    var event =
        PaymentWebhookEvent.received(
            null,
            "mock-psp",
            "evt_conflict",
            "evt_conflict",
            "{\"eventType\":\"PAYMENT_CONFIRMED\"}");
    var request =
        new PaymentWebhookRequest(
            "evt_conflict", "PAYMENT_CONFIRMED", "psp_ref_conflict", "2026-03-16T18:50:00Z", null);

    when(paymentRepository.findByProviderNameAndProviderReference("mock-psp", "psp_ref_conflict"))
        .thenReturn(Optional.of(payment));

    assertThatThrownBy(() -> processor.process(event, request))
        .isInstanceOf(PaymentConfirmationConflictException.class)
        .hasMessage("Payment confirmation is invalid for status FAILED.");
  }

  @Test
  void shouldIgnoreNonConfirmationEventTypes() {
    var event =
        PaymentWebhookEvent.received(
            null, "mock-psp", "evt_ignored", "evt_ignored", "{\"eventType\":\"PAYMENT_RECEIVED\"}");
    var request =
        new PaymentWebhookRequest("evt_ignored", "PAYMENT_RECEIVED", "psp_ref", null, null);

    processor.process(event, request);

    verify(paymentRepository, org.mockito.Mockito.never())
        .findByProviderNameAndProviderReference(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldAllowAlreadyConfirmedPaymentWithoutChangingConfirmedAt() {
    var order = order();
    var payment =
        Payment.create(
            UUID.randomUUID(), order, PaymentMethod.PIX, "mock-psp", "psp_ref_123", "payload");
    var confirmedAt = OffsetDateTime.parse("2026-03-16T18:50:00Z");
    payment.markConfirmed(confirmedAt);
    var event =
        PaymentWebhookEvent.received(
            null, "mock-psp", "evt_001", "evt_001", "{\"eventType\":\"PAYMENT_CONFIRMED\"}");
    var request =
        new PaymentWebhookRequest("evt_001", "PAYMENT_CONFIRMED", "psp_ref_123", null, null);

    when(paymentRepository.findByProviderNameAndProviderReference("mock-psp", "psp_ref_123"))
        .thenReturn(Optional.of(payment));

    processor.process(event, request);

    assertThat(payment.getConfirmedAt()).isEqualTo(confirmedAt);
    assertThat(event.getPayment()).isEqualTo(payment);
    verify(paymentRepository).save(payment);
  }

  @Test
  void shouldThrowGenericNotFoundWhenProviderReferenceIsMissing() {
    var event =
        PaymentWebhookEvent.received(
            null, "mock-psp", "evt_404", "evt_404", "{\"eventType\":\"PAYMENT_CONFIRMED\"}");
    var request = new PaymentWebhookRequest("evt_404", "PAYMENT_CONFIRMED", " ", null, null);

    assertThatThrownBy(() -> processor.process(event, request))
        .isInstanceOf(PaymentWebhookPaymentNotFoundException.class)
        .hasMessage("Payment not found for the informed provider and reference.");

    verify(paymentRepository, never())
        .findByProviderNameAndProviderReference(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldUseCurrentTimeWhenPaidAtIsMissing() {
    var order = order();
    var payment =
        Payment.create(
            UUID.randomUUID(), order, PaymentMethod.PIX, "mock-psp", "psp_ref_now", "payload");
    var event =
        PaymentWebhookEvent.received(
            null, "mock-psp", "evt_now", "evt_now", "{\"eventType\":\"PAYMENT_CONFIRMED\"}");
    var request =
        new PaymentWebhookRequest("evt_now", "PAYMENT_CONFIRMED", "psp_ref_now", " ", null);

    when(paymentRepository.findByProviderNameAndProviderReference("mock-psp", "psp_ref_now"))
        .thenReturn(Optional.of(payment));

    processor.process(event, request);

    assertThat(payment.getConfirmedAt()).isNotNull();
  }

  private SalesOrder order() {
    return SalesOrder.create(
        UUID.randomUUID(),
        mock(Store.class),
        mock(Customer.class),
        FulfillmentType.DELIVERY,
        PaymentMethod.PIX,
        new BigDecimal("50.00"),
        new BigDecimal("7.50"),
        new BigDecimal("57.50"),
        null,
        null);
  }
}
