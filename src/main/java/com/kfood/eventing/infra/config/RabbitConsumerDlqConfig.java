package com.kfood.eventing.infra.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.eventing.rabbit", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(EventConsumerProperties.class)
public class RabbitConsumerDlqConfig {

  @Bean
  public TopicExchange internalEventsDlxExchange(EventConsumerProperties properties) {
    return new TopicExchange(properties.dlxExchange(), true, false);
  }

  @Bean
  public Queue orderCreatedDlqQueue(RabbitTopologyProperties properties) {
    return new Queue(properties.orderCreated().queue() + ".dlq", true);
  }

  @Bean
  public Queue orderStatusChangedDlqQueue(RabbitTopologyProperties properties) {
    return new Queue(properties.orderStatusChanged().queue() + ".dlq", true);
  }

  @Bean
  public Queue paymentConfirmedDlqQueue(RabbitTopologyProperties properties) {
    return new Queue(properties.paymentConfirmed().queue() + ".dlq", true);
  }

  @Bean
  public Binding orderCreatedDlqBinding(
      Queue orderCreatedDlqQueue,
      TopicExchange internalEventsDlxExchange,
      EventConsumerProperties consumerProperties,
      RabbitTopologyProperties topologyProperties) {
    return BindingBuilder.bind(orderCreatedDlqQueue)
        .to(internalEventsDlxExchange)
        .with(
            consumerProperties.dlqRoutingPrefix() + topologyProperties.orderCreated().routingKey());
  }

  @Bean
  public Binding orderStatusChangedDlqBinding(
      Queue orderStatusChangedDlqQueue,
      TopicExchange internalEventsDlxExchange,
      EventConsumerProperties consumerProperties,
      RabbitTopologyProperties topologyProperties) {
    return BindingBuilder.bind(orderStatusChangedDlqQueue)
        .to(internalEventsDlxExchange)
        .with(
            consumerProperties.dlqRoutingPrefix()
                + topologyProperties.orderStatusChanged().routingKey());
  }

  @Bean
  public Binding paymentConfirmedDlqBinding(
      Queue paymentConfirmedDlqQueue,
      TopicExchange internalEventsDlxExchange,
      EventConsumerProperties consumerProperties,
      RabbitTopologyProperties topologyProperties) {
    return BindingBuilder.bind(paymentConfirmedDlqQueue)
        .to(internalEventsDlxExchange)
        .with(
            consumerProperties.dlqRoutingPrefix()
                + topologyProperties.paymentConfirmed().routingKey());
  }
}
