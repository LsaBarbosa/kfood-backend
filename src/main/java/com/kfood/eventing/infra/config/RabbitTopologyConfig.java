package com.kfood.eventing.infra.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.eventing.rabbit", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RabbitTopologyProperties.class)
public class RabbitTopologyConfig {

  @Bean
  public TopicExchange internalEventsExchange(RabbitTopologyProperties properties) {
    return new TopicExchange(properties.exchange(), true, false);
  }

  @Bean
  public Queue orderCreatedQueue(RabbitTopologyProperties properties) {
    return new Queue(properties.orderCreated().queue(), true);
  }

  @Bean
  public Queue orderStatusChangedQueue(RabbitTopologyProperties properties) {
    return new Queue(properties.orderStatusChanged().queue(), true);
  }

  @Bean
  public Queue paymentConfirmedQueue(RabbitTopologyProperties properties) {
    return new Queue(properties.paymentConfirmed().queue(), true);
  }

  @Bean
  public Binding orderCreatedBinding(
      Queue orderCreatedQueue,
      TopicExchange internalEventsExchange,
      RabbitTopologyProperties properties) {
    return BindingBuilder.bind(orderCreatedQueue)
        .to(internalEventsExchange)
        .with(properties.orderCreated().routingKey());
  }

  @Bean
  public Binding orderStatusChangedBinding(
      Queue orderStatusChangedQueue,
      TopicExchange internalEventsExchange,
      RabbitTopologyProperties properties) {
    return BindingBuilder.bind(orderStatusChangedQueue)
        .to(internalEventsExchange)
        .with(properties.orderStatusChanged().routingKey());
  }

  @Bean
  public Binding paymentConfirmedBinding(
      Queue paymentConfirmedQueue,
      TopicExchange internalEventsExchange,
      RabbitTopologyProperties properties) {
    return BindingBuilder.bind(paymentConfirmedQueue)
        .to(internalEventsExchange)
        .with(properties.paymentConfirmed().routingKey());
  }

  @Bean
  public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
    var rabbitAdmin = new RabbitAdmin(connectionFactory);
    rabbitAdmin.setAutoStartup(true);
    rabbitAdmin.setIgnoreDeclarationExceptions(false);
    return rabbitAdmin;
  }
}
