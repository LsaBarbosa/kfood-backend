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

class RabbitConsumerDlqConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ConfigurationPropertiesAutoConfiguration.class,
                  ValidationAutoConfiguration.class))
          .withUserConfiguration(
              RabbitTopologyConfig.class,
              RabbitConsumerDlqConfig.class,
              RabbitStartupVerifierConfig.class)
          .withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
          .withPropertyValues(
              "app.eventing.rabbit.enabled=true",
              "app.eventing.rabbit.startup-verify=false",
              "app.eventing.rabbit.exchange=kfood.events",
              "app.eventing.rabbit.consumer.max-attempts=3",
              "app.eventing.rabbit.consumer.initial-interval-ms=1000",
              "app.eventing.rabbit.consumer.multiplier=2.0",
              "app.eventing.rabbit.consumer.max-interval-ms=10000",
              "app.eventing.rabbit.consumer.dlx-exchange=kfood.events.dlx",
              "app.eventing.rabbit.consumer.dlq-routing-prefix=dlq.",
              "app.eventing.rabbit.order-created.queue=kfood.order.created.q",
              "app.eventing.rabbit.order-created.routing-key=order.created",
              "app.eventing.rabbit.order-status-changed.queue=kfood.order.status.changed.q",
              "app.eventing.rabbit.order-status-changed.routing-key=order.status.changed",
              "app.eventing.rabbit.payment-confirmed.queue=kfood.payment.confirmed.q",
              "app.eventing.rabbit.payment-confirmed.routing-key=payment.confirmed");

  @Test
  void shouldCreateDlqQueuesAndBindings() {
    contextRunner.run(
        context -> {
          assertThat(context).hasBean("internalEventsDlxExchange");
          assertThat(context.getBeansOfType(TopicExchange.class)).hasSize(2);
          assertThat(context.getBeansOfType(Queue.class)).hasSize(6);
          assertThat(context.getBeansOfType(Binding.class)).hasSize(6);

          var paymentConfirmedDlqQueue = context.getBean("paymentConfirmedDlqQueue", Queue.class);
          var paymentConfirmedDlqBinding =
              context.getBean("paymentConfirmedDlqBinding", Binding.class);
          var dlxExchange = context.getBean("internalEventsDlxExchange", TopicExchange.class);

          assertThat(paymentConfirmedDlqQueue.getName()).isEqualTo("kfood.payment.confirmed.q.dlq");
          assertThat(paymentConfirmedDlqBinding.getRoutingKey()).isEqualTo("dlq.payment.confirmed");
          assertThat(dlxExchange.getName()).isEqualTo("kfood.events.dlx");
        });
  }
}
