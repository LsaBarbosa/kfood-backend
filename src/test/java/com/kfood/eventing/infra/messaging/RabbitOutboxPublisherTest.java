package com.kfood.eventing.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.kfood.eventing.infra.config.RabbitTopologyProperties;
import com.kfood.eventing.infra.persistence.OutboxEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitOutboxPublisherTest {

  @Test
  void shouldPublishJsonPayloadWithPersistentHeaders() {
    var rabbitTemplate = mock(RabbitTemplate.class);
    var publisher =
        new RabbitOutboxPublisher(
            rabbitTemplate,
            new RabbitTopologyProperties(
                "kfood.events",
                true,
                new RabbitTopologyProperties.Route("kfood.order.created.q", "order.created"),
                new RabbitTopologyProperties.Route(
                    "kfood.order.status.changed.q", "order.status.changed"),
                new RabbitTopologyProperties.Route(
                    "kfood.payment.confirmed.q", "payment.confirmed")));
    var outboxEvent =
        OutboxEvent.newPending(
            "ORDER", "101", "order.created", "order.created", "{\"eventType\":\"order.created\"}");

    publisher.publish(outboxEvent);

    var postProcessorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(rabbitTemplate)
        .convertAndSend(
            eq("kfood.events"),
            eq("order.created"),
            eq("{\"eventType\":\"order.created\"}"),
            postProcessorCaptor.capture());

    var message = new Message(new byte[0], new MessageProperties());
    var processedMessage = postProcessorCaptor.getValue().postProcessMessage(message);

    assertThat(processedMessage.getMessageProperties().getContentType())
        .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    assertThat(processedMessage.getMessageProperties().getDeliveryMode())
        .isEqualTo(MessageDeliveryMode.PERSISTENT);
    assertThat(processedMessage.getMessageProperties().getHeaders())
        .containsEntry("event_type", "order.created")
        .containsEntry("outbox_id", outboxEvent.getId().toString());
  }
}
