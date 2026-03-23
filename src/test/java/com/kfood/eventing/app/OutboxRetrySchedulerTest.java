package com.kfood.eventing.app;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class OutboxRetrySchedulerTest {

  @Test
  void shouldDelegatePendingBatchPublishing() {
    var outboxRelayService = mock(OutboxRelayService.class);
    var scheduler = new OutboxRetryScheduler(outboxRelayService);

    scheduler.publishPendingEvents();

    verify(outboxRelayService).publishPendingBatch();
  }
}
