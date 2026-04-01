package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.app.port.PaymentWebhookPaymentPort;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessConfirmedPaymentWebhookUseCaseTest {

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort =
      mock(PaymentWebhookEventPersistencePort.class);
  private final PaymentWebhookPaymentPort paymentWebhookPaymentPort =
      mock(PaymentWebhookPaymentPort.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-30T18:00:00Z"), ZoneOffset.UTC);
  private final ProcessConfirmedPaymentWebhookUseCase useCase =
      new ProcessConfirmedPaymentWebhookUseCase(
          paymentWebhookEventPersistencePort, paymentWebhookPaymentPort, clock);

  @Test
  void shouldConfirmPaymentUpdateOrderSnapshotAndMarkEventProcessed() {
    var event = webhookEvent();
    var payment = payment(PaymentStatus.PENDING);
    var processedEvent = webhookEvent();
    processedEvent.attachPayment(payment);
    processedEvent.markProcessed(Instant.now(clock));

    when(paymentWebhookPaymentPort.findByProviderNameAndProviderReference("mock", "charge-123"))
        .thenReturn(Optional.of(payment));
    when(paymentWebhookEventPersistencePort.markProcessed(
            event.getId(), payment.getId(), Instant.now(clock)))
        .thenReturn(processedEvent);

    var result = useCase.execute(event, "charge-123");

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(payment.getConfirmedAt()).isEqualTo(Instant.now(clock));
    assertThat(((SalesOrder) payment.getOrder()).getPaymentStatusSnapshot())
        .isEqualTo(PaymentStatusSnapshot.PAID);
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    verify(paymentWebhookEventPersistencePort)
        .markProcessed(event.getId(), payment.getId(), Instant.now(clock));
  }

  @Test
  void shouldKeepConfirmationIdempotentWhenPaymentIsAlreadyConfirmed() {
    var event = webhookEvent();
    var payment = payment(PaymentStatus.CONFIRMED);
    var confirmedAt = payment.getConfirmedAt();
    var processedEvent = webhookEvent();
    processedEvent.attachPayment(payment);
    processedEvent.markProcessed(Instant.now(clock));

    when(paymentWebhookPaymentPort.findByProviderNameAndProviderReference("mock", "charge-123"))
        .thenReturn(Optional.of(payment));
    when(paymentWebhookEventPersistencePort.markProcessed(
            event.getId(), payment.getId(), Instant.now(clock)))
        .thenReturn(processedEvent);

    useCase.execute(event, "charge-123");

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(payment.getConfirmedAt()).isEqualTo(confirmedAt);
    assertThat(((SalesOrder) payment.getOrder()).getPaymentStatusSnapshot())
        .isEqualTo(PaymentStatusSnapshot.PAID);
  }

  @Test
  void shouldMarkEventAsFailedWhenProviderReferenceIsMissing() {
    var event = webhookEvent();
    var failedEvent = failedEvent();
    when(paymentWebhookEventPersistencePort.markFailed(event.getId(), Instant.now(clock)))
        .thenReturn(failedEvent);

    var result = useCase.execute(event, null);

    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.FAILED);
    verify(paymentWebhookPaymentPort, never())
        .findByProviderNameAndProviderReference("mock", "charge-123");
  }

  @Test
  void shouldMarkEventAsFailedWhenProviderReferenceIsBlank() {
    var event = webhookEvent();
    var failedEvent = failedEvent();
    when(paymentWebhookEventPersistencePort.markFailed(event.getId(), Instant.now(clock)))
        .thenReturn(failedEvent);

    var result = useCase.execute(event, "   ");

    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.FAILED);
    verify(paymentWebhookPaymentPort, never())
        .findByProviderNameAndProviderReference("mock", "charge-123");
  }

  @Test
  void shouldMarkEventAsFailedWhenPaymentCannotBeCorrelated() {
    var event = webhookEvent();
    var failedEvent = failedEvent();
    when(paymentWebhookPaymentPort.findByProviderNameAndProviderReference("mock", "charge-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.markFailed(event.getId(), Instant.now(clock)))
        .thenReturn(failedEvent);

    var result = useCase.execute(event, "charge-123");

    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.FAILED);
    verify(paymentWebhookEventPersistencePort).markFailed(event.getId(), Instant.now(clock));
  }

  @Test
  void shouldMarkEventAsFailedWhenPaymentTransitionIsInvalid() {
    var event = webhookEvent();
    var failedEvent = failedEvent();
    var payment = payment(PaymentStatus.FAILED);
    when(paymentWebhookPaymentPort.findByProviderNameAndProviderReference("mock", "charge-123"))
        .thenReturn(Optional.of(payment));
    when(paymentWebhookEventPersistencePort.markFailed(event.getId(), Instant.now(clock)))
        .thenReturn(failedEvent);

    var result = useCase.execute(event, "charge-123");

    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.FAILED);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(((SalesOrder) payment.getOrder()).getPaymentStatusSnapshot())
        .isEqualTo(PaymentStatusSnapshot.FAILED);
  }

  private PaymentWebhookEvent webhookEvent() {
    return new PaymentWebhookEvent(
        UUID.randomUUID(),
        null,
        "mock",
        "evt-123",
        "PAYMENT_CONFIRMED",
        false,
        "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
        Instant.parse("2026-03-30T17:00:00Z"));
  }

  private PaymentWebhookEvent failedEvent() {
    var event = webhookEvent();
    event.markFailed(Instant.now(clock));
    return event;
  }

  private Payment payment(PaymentStatus status) {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var customer =
        new Customer(UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com");
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            new BigDecimal("7.50"),
            new BigDecimal("57.50"),
            null,
            null);
    var payment =
        new Payment(
            UUID.randomUUID(),
            order,
            PaymentMethod.PIX,
            "mock",
            "charge-123",
            PaymentStatus.PENDING,
            new BigDecimal("57.50"),
            "000201...",
            null,
            null);
    if (status != PaymentStatus.PENDING) {
      payment.changeStatus(status, Instant.parse("2026-03-30T17:30:00Z"));
      order.markPaymentStatusSnapshot(PaymentStatusSnapshotMapper.from(payment.getStatus()));
    }
    return payment;
  }
}
