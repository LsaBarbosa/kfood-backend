package com.kfood.eventing.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.eventing.domain.OutboxEventStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

  @Test
  void shouldMarkEventAsPublished() {
    var event =
        OutboxEvent.newPending(
            "ORDER", "101", "order.created", "order.created", "{\"eventType\":\"order.created\"}");
    var publishedAt = Instant.parse("2026-03-23T12:00:00Z");

    event.markPublished(publishedAt);

    assertThat(event.getPublicationStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    assertThat(event.getPublishedAt()).isEqualTo(publishedAt);
    assertThat(event.getLastError()).isNull();
  }

  @Test
  void shouldKeepEventPendingAndTruncateFailureMessage() {
    var event =
        OutboxEvent.newPending(
            "ORDER", "101", "order.created", "order.created", "{\"eventType\":\"order.created\"}");
    var longMessage = "x".repeat(700);

    event.registerFailure(longMessage);

    assertThat(event.getPublicationStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(event.getAttempts()).isEqualTo(1);
    assertThat(event.getLastError()).hasSize(500);
    assertThat(event.getPublishedAt()).isNull();
  }
}
