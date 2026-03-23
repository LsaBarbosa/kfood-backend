package com.kfood.eventing.app;

import com.kfood.eventing.domain.OutboxEventStatus;
import com.kfood.eventing.infra.config.EventingOutboxProperties;
import com.kfood.eventing.infra.config.RabbitTopologyProperties;
import com.kfood.eventing.infra.messaging.RabbitOutboxPublisher;
import com.kfood.eventing.infra.persistence.OutboxEvent;
import com.kfood.eventing.infra.persistence.OutboxEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(RabbitTopologyProperties.class)
public class OutboxRelayService {

  private final OutboxEventRepository outboxEventRepository;
  private final RabbitOutboxPublisher rabbitOutboxPublisher;
  private final EventingOutboxProperties eventingOutboxProperties;

  public OutboxRelayService(
      OutboxEventRepository outboxEventRepository,
      RabbitOutboxPublisher rabbitOutboxPublisher,
      EventingOutboxProperties eventingOutboxProperties) {
    this.outboxEventRepository = outboxEventRepository;
    this.rabbitOutboxPublisher = rabbitOutboxPublisher;
    this.eventingOutboxProperties = eventingOutboxProperties;
  }

  public void publishById(UUID outboxEventId) {
    outboxEventRepository
        .findById(outboxEventId)
        .filter(event -> event.getPublicationStatus() == OutboxEventStatus.PENDING)
        .ifPresent(this::publishSafely);
  }

  public void publishPendingBatch() {
    var pageRequest = PageRequest.of(0, eventingOutboxProperties.batchSize());

    outboxEventRepository
        .findByPublicationStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, pageRequest)
        .getContent()
        .forEach(this::publishSafely);
  }

  private void publishSafely(OutboxEvent outboxEvent) {
    try {
      rabbitOutboxPublisher.publish(outboxEvent);
      outboxEvent.markPublished(Instant.now());
    } catch (Exception exception) {
      outboxEvent.registerFailure(exception.getMessage());
    }

    outboxEventRepository.save(outboxEvent);
  }
}
