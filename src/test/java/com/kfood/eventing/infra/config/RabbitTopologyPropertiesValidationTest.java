package com.kfood.eventing.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

class RabbitTopologyPropertiesValidationTest {

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
              "app.eventing.rabbit.order-created.queue=kfood.order.created.q",
              "app.eventing.rabbit.order-created.routing-key=order.created",
              "app.eventing.rabbit.order-status-changed.queue=kfood.order.status.changed.q",
              "app.eventing.rabbit.order-status-changed.routing-key=order.status.changed",
              "app.eventing.rabbit.payment-confirmed.queue=kfood.payment.confirmed.q",
              "app.eventing.rabbit.payment-confirmed.routing-key=payment.confirmed");

  @Test
  void shouldFailFastWhenExchangeConfigurationIsMissing() {
    contextRunner.run(
        context -> {
          assertThat(context).hasFailed();
          var startupFailure = context.getStartupFailure();
          var rootCause = startupFailure;

          while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
          }

          assertThat(startupFailure)
              .hasMessageContaining("Could not bind properties to 'RabbitTopologyProperties'");
          assertThat(rootCause)
              .hasMessageContaining("Binding validation errors on app.eventing.rabbit");
        });
  }
}
