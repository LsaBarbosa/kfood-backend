package com.kfood.eventing.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.eventing.infra.persistence.OutboxEvent;
import com.kfood.eventing.infra.persistence.OutboxEventRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentConfirmedOutboxServiceTest {

  private OutboxEventRepository outboxEventRepository;
  private CorrelationIdProvider correlationIdProvider;
  private ObjectMapper objectMapper;
  private PaymentConfirmedOutboxService service;

  @BeforeEach
  void setUp() {
    outboxEventRepository = mock(OutboxEventRepository.class);
    correlationIdProvider = mock(CorrelationIdProvider.class);
    objectMapper = new ObjectMapper().findAndRegisterModules();

    when(correlationIdProvider.getOrCreate()).thenReturn("corr-pay-1");
    when(outboxEventRepository.save(any(OutboxEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service =
        new PaymentConfirmedOutboxService(
            outboxEventRepository, objectMapper, correlationIdProvider);
  }

  @Test
  void shouldCreatePaymentConfirmedEventOnlyOnce() {
    var facts =
        new PaymentConfirmedFacts(
            "pay-1",
            "ord-10",
            "store-1",
            "mock-psp",
            new BigDecimal("57.90"),
            OffsetDateTime.parse("2026-03-23T12:00:00Z"));

    when(outboxEventRepository.findByDedupKey("payment.confirmed:pay-1"))
        .thenReturn(Optional.empty());

    var firstId = service.enqueueOnce(facts);
    var existing =
        OutboxEvent.newPending(
            "PAYMENT",
            "pay-1",
            "payment.confirmed",
            "payment.confirmed",
            "{\"stub\":true}",
            "payment.confirmed:pay-1");

    when(outboxEventRepository.findByDedupKey("payment.confirmed:pay-1"))
        .thenReturn(Optional.of(existing));

    var secondId = service.enqueueOnce(facts);

    assertThat(secondId).isEqualTo(existing.getId());
    assertThat(firstId).isNotNull();
    verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
  }

  @Test
  void shouldBuildMinimumPayload() throws Exception {
    var facts =
        new PaymentConfirmedFacts(
            "pay-1",
            "ord-10",
            "store-1",
            "mock-psp",
            new BigDecimal("57.90"),
            OffsetDateTime.parse("2026-03-23T12:00:00Z"));

    when(outboxEventRepository.findByDedupKey("payment.confirmed:pay-1"))
        .thenReturn(Optional.empty());

    service.enqueueOnce(facts);

    var captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());

    var event = captor.getValue();
    var root = objectMapper.readTree(event.getPayload());
    var payload = root.get("payload");

    assertThat(event.getDedupKey()).isEqualTo("payment.confirmed:pay-1");
    assertThat(root.get("eventType").asText()).isEqualTo("payment.confirmed");
    assertThat(payload.get("paymentId").asText()).isEqualTo("pay-1");
    assertThat(payload.get("orderId").asText()).isEqualTo("ord-10");
    assertThat(payload.get("providerName").asText()).isEqualTo("mock-psp");
    assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("57.90");
  }
}
