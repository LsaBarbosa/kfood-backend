package com.kfood.eventing.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

class RabbitTopologyConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ConfigurationPropertiesAutoConfiguration.class,
                  ValidationAutoConfiguration.class))
          .withUserConfiguration(RabbitTopologyConfig.class, RabbitStartupVerifierConfig.class)
          .withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
          .withPropertyValues(
              "app.eventing.rabbit.enabled=true",
              "app.eventing.rabbit.startup-verify=false",
              "app.eventing.rabbit.exchange=kfood.events",
              "app.eventing.rabbit.order-created.queue=kfood.order.created.q",
              "app.eventing.rabbit.order-created.routing-key=order.created",
              "app.eventing.rabbit.order-status-changed.queue=kfood.order.status.changed.q",
              "app.eventing.rabbit.order-status-changed.routing-key=order.status.changed",
              "app.eventing.rabbit.payment-confirmed.queue=kfood.payment.confirmed.q",
              "app.eventing.rabbit.payment-confirmed.routing-key=payment.confirmed");

  @Test
  void shouldCreateExchangeQueueAndBindingBeans() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(TopicExchange.class);
          assertThat(context).hasBean("orderCreatedQueue");
          assertThat(context).hasBean("orderStatusChangedQueue");
          assertThat(context).hasBean("paymentConfirmedQueue");
          assertThat(context).hasSingleBean(RabbitTopologyProperties.class);

          assertThat(context.getBeansOfType(Queue.class)).hasSize(3);
          assertThat(context.getBeansOfType(Binding.class)).hasSize(3);

          var exchange = context.getBean(TopicExchange.class);
          var orderCreatedQueue = context.getBean("orderCreatedQueue", Queue.class);
          var orderStatusChangedQueue = context.getBean("orderStatusChangedQueue", Queue.class);
          var paymentConfirmedQueue = context.getBean("paymentConfirmedQueue", Queue.class);
          var orderCreatedBinding = context.getBean("orderCreatedBinding", Binding.class);
          var orderStatusChangedBinding =
              context.getBean("orderStatusChangedBinding", Binding.class);
          var paymentConfirmedBinding = context.getBean("paymentConfirmedBinding", Binding.class);

          assertThat(exchange.getName()).isEqualTo("kfood.events");
          assertThat(orderCreatedQueue.getName()).isEqualTo("kfood.order.created.q");
          assertThat(orderStatusChangedQueue.getName()).isEqualTo("kfood.order.status.changed.q");
          assertThat(paymentConfirmedQueue.getName()).isEqualTo("kfood.payment.confirmed.q");
          assertThat(orderCreatedBinding.getRoutingKey()).isEqualTo("order.created");
          assertThat(orderStatusChangedBinding.getRoutingKey()).isEqualTo("order.status.changed");
          assertThat(paymentConfirmedBinding.getRoutingKey()).isEqualTo("payment.confirmed");
        });
  }
}
