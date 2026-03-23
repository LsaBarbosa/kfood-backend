package com.kfood.eventing.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

class EventConsumerPropertiesValidationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ConfigurationPropertiesAutoConfiguration.class,
                  ValidationAutoConfiguration.class))
          .withUserConfiguration(RabbitConsumerListenerConfig.class)
          .withPropertyValues(
              "app.eventing.rabbit.enabled=true",
              "app.eventing.rabbit.consumer.initial-interval-ms=1000",
              "app.eventing.rabbit.consumer.multiplier=2.0",
              "app.eventing.rabbit.consumer.max-interval-ms=10000",
              "app.eventing.rabbit.consumer.dlx-exchange=kfood.events.dlx",
              "app.eventing.rabbit.consumer.dlq-routing-prefix=dlq.");

  @Test
  void shouldFailFastWhenMaxAttemptsIsMissingOrInvalid() {
    contextRunner
        .withBean(
            org.springframework.amqp.rabbit.connection.ConnectionFactory.class,
            () ->
                org.mockito.Mockito.mock(
                    org.springframework.amqp.rabbit.connection.ConnectionFactory.class))
        .withBean(
            org.springframework.boot.amqp.autoconfigure
                .SimpleRabbitListenerContainerFactoryConfigurer.class,
            () ->
                org.mockito.Mockito.mock(
                    org.springframework.boot.amqp.autoconfigure
                        .SimpleRabbitListenerContainerFactoryConfigurer.class))
        .withBean(
            org.springframework.amqp.rabbit.core.RabbitTemplate.class,
            () ->
                org.mockito.Mockito.mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class))
        .withPropertyValues("app.eventing.rabbit.consumer.max-attempts=0")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasMessageContaining("Could not bind properties to 'EventConsumerProperties'");
            });
  }
}
