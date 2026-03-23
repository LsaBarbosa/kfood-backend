package com.kfood.eventing.app;

import com.kfood.eventing.infra.config.RabbitTopologyProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnBean(RabbitTopologyProperties.class)
public class OutboxAfterCommitPublisher {

  private final OutboxRelayService outboxRelayService;

  public OutboxAfterCommitPublisher(OutboxRelayService outboxRelayService) {
    this.outboxRelayService = outboxRelayService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(OutboxCreatedEvent event) {
    outboxRelayService.publishById(event.outboxEventId());
  }
}
