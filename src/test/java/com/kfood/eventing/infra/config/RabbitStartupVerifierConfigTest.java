package com.kfood.eventing.infra.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.ApplicationArguments;

class RabbitStartupVerifierConfigTest {

  private final RabbitStartupVerifierConfig config = new RabbitStartupVerifierConfig();

  @Test
  void shouldInitializeTopologyAndCloseConnectionWhenBrokerIsAvailable() throws Exception {
    var rabbitAdmin = mock(RabbitAdmin.class);
    var connectionFactory = mock(ConnectionFactory.class);
    var connection = mock(Connection.class);
    var applicationArguments = mock(ApplicationArguments.class);

    when(connectionFactory.createConnection()).thenReturn(connection);
    when(connection.isOpen()).thenReturn(true);

    var runner = config.rabbitStartupVerifier(rabbitAdmin, connectionFactory);

    runner.run(applicationArguments);

    verify(rabbitAdmin).initialize();
    verify(connectionFactory).createConnection();
    verify(connection).isOpen();
    verify(connection).close();
  }

  @Test
  void shouldFailWhenRabbitConnectionIsClosed() {
    var rabbitAdmin = mock(RabbitAdmin.class);
    var connectionFactory = mock(ConnectionFactory.class);
    var connection = mock(Connection.class);
    var applicationArguments = mock(ApplicationArguments.class);

    when(connectionFactory.createConnection()).thenReturn(connection);
    when(connection.isOpen()).thenReturn(false);

    var runner = config.rabbitStartupVerifier(rabbitAdmin, connectionFactory);

    assertThatThrownBy(() -> runner.run(applicationArguments))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("RabbitMQ connection is not open");

    verify(rabbitAdmin).initialize();
    verify(connection).close();
  }
}
