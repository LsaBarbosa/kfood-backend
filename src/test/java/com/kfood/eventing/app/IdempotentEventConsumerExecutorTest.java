package com.kfood.eventing.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.eventing.infra.persistence.ProcessedEventRepository;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IdempotentEventConsumerExecutorTest {

  private final ProcessedEventRepository processedEventRepository =
      org.mockito.Mockito.mock(ProcessedEventRepository.class);

  private final IdempotentEventConsumerExecutor executor =
      new IdempotentEventConsumerExecutor(processedEventRepository);

  @Test
  void shouldSkipSideEffectWhenEventWasAlreadyProcessed() {
    var eventId = UUID.randomUUID();
    var metadata = new ConsumedEventMetadata(eventId, "payment.confirmed", "pay-1");

    when(processedEventRepository.existsByConsumerNameAndEventId("audit-consumer", eventId))
        .thenReturn(true);

    executor.execute("audit-consumer", metadata, () -> assertThat(false).isTrue());

    verify(processedEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldExecuteSideEffectAndPersistProcessedEvent() {
    var eventId = UUID.randomUUID();
    var metadata = new ConsumedEventMetadata(eventId, "payment.confirmed", "pay-1");
    var sideEffects = new AtomicInteger();

    when(processedEventRepository.existsByConsumerNameAndEventId("audit-consumer", eventId))
        .thenReturn(false);

    executor.execute("audit-consumer", metadata, sideEffects::incrementAndGet);

    var processedEventCaptor =
        ArgumentCaptor.forClass(com.kfood.eventing.infra.persistence.ProcessedEvent.class);

    verify(processedEventRepository).save(processedEventCaptor.capture());
    assertThat(sideEffects.get()).isEqualTo(1);
    assertThat(processedEventCaptor.getValue().getConsumerName()).isEqualTo("audit-consumer");
    assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(eventId);
    assertThat(processedEventCaptor.getValue().getEventType()).isEqualTo("payment.confirmed");
    assertThat(processedEventCaptor.getValue().getAggregateId()).isEqualTo("pay-1");
  }
}
