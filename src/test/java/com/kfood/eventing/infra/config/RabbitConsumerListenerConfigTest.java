package com.kfood.eventing.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

class RabbitConsumerListenerConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ConfigurationPropertiesAutoConfiguration.class,
                  ValidationAutoConfiguration.class))
          .withUserConfiguration(RabbitConsumerListenerConfig.class)
          .withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
          .withBean(
              SimpleRabbitListenerContainerFactoryConfigurer.class,
              () -> mock(SimpleRabbitListenerContainerFactoryConfigurer.class))
          .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
          .withPropertyValues(
              "app.eventing.rabbit.enabled=true",
              "app.eventing.rabbit.consumer.max-attempts=3",
              "app.eventing.rabbit.consumer.initial-interval-ms=1000",
              "app.eventing.rabbit.consumer.multiplier=2.0",
              "app.eventing.rabbit.consumer.max-interval-ms=10000",
              "app.eventing.rabbit.consumer.dlx-exchange=kfood.events.dlx",
              "app.eventing.rabbit.consumer.dlq-routing-prefix=dlq.");

  @Test
  void shouldCreateListenerFactoryAndRetryInterceptor() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(EventConsumerProperties.class);
          assertThat(context).hasBean("internalEventRabbitListenerContainerFactory");
          assertThat(context).hasBean("internalEventRetryInterceptor");
          assertThat(context.getBean("internalEventRetryInterceptor"))
              .isInstanceOf(MethodInterceptor.class);
        });
  }
}
