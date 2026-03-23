package com.kfood.eventing.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessedEventTest {

  @Test
  void shouldCreateProcessedEventWithTrimmedFields() {
    var eventId = UUID.randomUUID();

    var processedEvent =
        ProcessedEvent.create(" consumer ", eventId, " payment.confirmed ", " pay-1 ");

    assertThat(processedEvent.getId()).isNotNull();
    assertThat(processedEvent.getConsumerName()).isEqualTo("consumer");
    assertThat(processedEvent.getEventId()).isEqualTo(eventId);
    assertThat(processedEvent.getEventType()).isEqualTo("payment.confirmed");
    assertThat(processedEvent.getAggregateId()).isEqualTo("pay-1");
    assertThat(processedEvent.getProcessedAt()).isNotNull();
  }

  @Test
  void shouldRejectBlankConsumerName() {
    var eventId = UUID.randomUUID();

    assertThatThrownBy(() -> ProcessedEvent.create(" ", eventId, "payment.confirmed", "pay-1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("consumerName must not be blank");
  }
}
