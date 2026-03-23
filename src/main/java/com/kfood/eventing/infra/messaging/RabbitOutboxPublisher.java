package com.kfood.eventing.infra.messaging;

import com.kfood.eventing.infra.config.RabbitTopologyProperties;
import com.kfood.eventing.infra.persistence.OutboxEvent;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(RabbitTopologyProperties.class)
public class RabbitOutboxPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final RabbitTopologyProperties rabbitTopologyProperties;

  public RabbitOutboxPublisher(
      RabbitTemplate rabbitTemplate, RabbitTopologyProperties rabbitTopologyProperties) {
    this.rabbitTemplate = rabbitTemplate;
    this.rabbitTopologyProperties = rabbitTopologyProperties;
  }

  public void publish(OutboxEvent outboxEvent) {
    rabbitTemplate.convertAndSend(
        rabbitTopologyProperties.exchange(),
        outboxEvent.getRoutingKey(),
        outboxEvent.getPayload(),
        message -> {
          var properties = message.getMessageProperties();
          properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
          properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          properties.setHeader("event_type", outboxEvent.getEventType());
          properties.setHeader("outbox_id", outboxEvent.getId().toString());
          return message;
        });
  }
}
