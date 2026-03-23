package com.kfood.eventing.infra.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.eventing.rabbit", name = "enabled", havingValue = "true")
public class RabbitStartupVerifierConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "app.eventing.rabbit",
      name = "startup-verify",
      havingValue = "true",
      matchIfMissing = true)
  public ApplicationRunner rabbitStartupVerifier(
      RabbitAdmin rabbitAdmin, ConnectionFactory connectionFactory) {
    return args -> {
      rabbitAdmin.initialize();

      var connection = connectionFactory.createConnection();
      try {
        if (!connection.isOpen()) {
          throw new IllegalStateException("RabbitMQ connection is not open");
        }
      } finally {
        connection.close();
      }
    };
  }
}
