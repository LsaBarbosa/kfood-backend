package com.kfood.eventing.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RabbitMqConnectionIT {

  @Container
  static final RabbitMQContainer rabbitMqContainer =
      new RabbitMQContainer("rabbitmq:3.13-management");

  @Test
  void shouldStartApplicationWithBrokerAvailable() {
    try (var context = applicationContext()) {
      var connectionFactory = context.getBean(ConnectionFactory.class);
      var amqpAdmin = context.getBean(AmqpAdmin.class);
      var connection = connectionFactory.createConnection();

      try {
        assertThat(connection.isOpen()).isTrue();
      } finally {
        connection.close();
      }

      assertThat(amqpAdmin.getQueueProperties("kfood.order.created.q")).isNotNull();
      assertThat(amqpAdmin.getQueueProperties("kfood.order.status.changed.q")).isNotNull();
      assertThat(amqpAdmin.getQueueProperties("kfood.payment.confirmed.q")).isNotNull();
    }
  }

  private ConfigurableApplicationContext applicationContext() {
    return new SpringApplicationBuilder(TestApplication.class)
        .web(WebApplicationType.NONE)
        .properties(properties())
        .run();
  }

  private Map<String, Object> properties() {
    return Map.ofEntries(
        Map.entry("spring.rabbitmq.host", rabbitMqContainer.getHost()),
        Map.entry("spring.rabbitmq.port", rabbitMqContainer.getAmqpPort()),
        Map.entry("spring.rabbitmq.username", rabbitMqContainer.getAdminUsername()),
        Map.entry("spring.rabbitmq.password", rabbitMqContainer.getAdminPassword()),
        Map.entry("app.eventing.rabbit.enabled", true),
        Map.entry("app.eventing.rabbit.startup-verify", true),
        Map.entry("app.eventing.rabbit.exchange", "kfood.events"),
        Map.entry("app.eventing.rabbit.order-created.queue", "kfood.order.created.q"),
        Map.entry("app.eventing.rabbit.order-created.routing-key", "order.created"),
        Map.entry("app.eventing.rabbit.order-status-changed.queue", "kfood.order.status.changed.q"),
        Map.entry("app.eventing.rabbit.order-status-changed.routing-key", "order.status.changed"),
        Map.entry("app.eventing.rabbit.payment-confirmed.queue", "kfood.payment.confirmed.q"),
        Map.entry("app.eventing.rabbit.payment-confirmed.routing-key", "payment.confirmed"));
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration(
      excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
      })
  @Import({RabbitTopologyConfig.class, RabbitStartupVerifierConfig.class})
  static class TestApplication {}
}
