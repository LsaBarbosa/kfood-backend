package com.kfood.eventing.app;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxAfterCommitPublisherTest {

  @Test
  void shouldDelegatePublishingByOutboxId() {
    var outboxRelayService = mock(OutboxRelayService.class);
    var publisher = new OutboxAfterCommitPublisher(outboxRelayService);
    var outboxId = UUID.randomUUID();

    publisher.handle(new OutboxCreatedEvent(outboxId));

    verify(outboxRelayService).publishById(outboxId);
  }
}
