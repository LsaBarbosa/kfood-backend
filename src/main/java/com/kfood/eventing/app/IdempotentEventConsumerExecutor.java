package com.kfood.eventing.app;

import com.kfood.eventing.infra.persistence.ProcessedEvent;
import com.kfood.eventing.infra.persistence.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotentEventConsumerExecutor {

  private final ProcessedEventRepository processedEventRepository;

  public IdempotentEventConsumerExecutor(ProcessedEventRepository processedEventRepository) {
    this.processedEventRepository = processedEventRepository;
  }

  @Transactional
  public void execute(String consumerName, ConsumedEventMetadata metadata, Runnable sideEffect) {
    var alreadyProcessed =
        processedEventRepository.existsByConsumerNameAndEventId(consumerName, metadata.eventId());

    if (alreadyProcessed) {
      return;
    }

    sideEffect.run();
    processedEventRepository.save(
        ProcessedEvent.create(
            consumerName, metadata.eventId(), metadata.eventType(), metadata.aggregateId()));
  }
}
