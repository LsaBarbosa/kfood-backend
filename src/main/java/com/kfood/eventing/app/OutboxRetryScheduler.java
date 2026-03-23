package com.kfood.eventing.app;

import com.kfood.eventing.infra.config.RabbitTopologyProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(RabbitTopologyProperties.class)
public class OutboxRetryScheduler {

  private final OutboxRelayService outboxRelayService;

  public OutboxRetryScheduler(OutboxRelayService outboxRelayService) {
    this.outboxRelayService = outboxRelayService;
  }

  @Scheduled(fixedDelayString = "${app.eventing.outbox.retry-interval:5000}")
  public void publishPendingEvents() {
    outboxRelayService.publishPendingBatch();
  }
}
