package com.kfood.payment.infra.eventing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.eventing.app.OutboxCreatedEvent;
import com.kfood.eventing.app.PaymentConfirmedFacts;
import com.kfood.eventing.app.PaymentConfirmedOutboxService;
import com.kfood.payment.app.PaymentConfirmedEvent;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class SpringPaymentConfirmedPublisherTest {

  @Test
  void shouldStorePaymentConfirmedInOutboxAndPublishInternalNotification() {
    var paymentConfirmedOutboxService = mock(PaymentConfirmedOutboxService.class);
    var applicationEventPublisher = mock(ApplicationEventPublisher.class);
    var publisher =
        new SpringPaymentConfirmedPublisher(
            paymentConfirmedOutboxService, applicationEventPublisher);
    var event =
        new PaymentConfirmedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "mock-psp",
            new BigDecimal("57.90"),
            OffsetDateTime.parse("2026-03-23T12:00:00Z"));
    var outboxId = UUID.randomUUID();

    when(paymentConfirmedOutboxService.enqueueOnce(
            org.mockito.ArgumentMatchers.any(PaymentConfirmedFacts.class)))
        .thenReturn(outboxId);

    publisher.publish(event);

    var factsCaptor = ArgumentCaptor.forClass(PaymentConfirmedFacts.class);
    verify(paymentConfirmedOutboxService).enqueueOnce(factsCaptor.capture());
    verify(applicationEventPublisher).publishEvent(new OutboxCreatedEvent(outboxId));

    var facts = factsCaptor.getValue();
    assertThat(facts.paymentId()).isEqualTo(event.paymentId().toString());
    assertThat(facts.orderId()).isEqualTo(event.orderId().toString());
    assertThat(facts.tenantId()).isEqualTo(event.storeId().toString());
    assertThat(facts.providerName()).isEqualTo("mock-psp");
    assertThat(facts.amount()).isEqualByComparingTo("57.90");
    assertThat(facts.occurredAt()).isEqualTo(OffsetDateTime.parse("2026-03-23T12:00:00Z"));
  }
}
