package com.kfood.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class LocalInfraComposeFileTest {
  private static final Path DOCKER_COMPOSE_PATH = Path.of("docker-compose.yml");

  @Test
  void shouldExistDockerComposeFile() {
    assertThat(DOCKER_COMPOSE_PATH).exists().isRegularFile();
  }

  @Test
  void shouldContainRequiredServices() throws IOException {
    String compose = Files.readString(DOCKER_COMPOSE_PATH);

    assertThat(compose).contains("postgres:");
    assertThat(compose).contains("redis:");
    assertThat(compose).contains("rabbitmq:");
  }

  @Test
  void shouldExposeExpectedPorts() throws IOException {
    String compose = Files.readString(DOCKER_COMPOSE_PATH);

    assertThat(compose).contains("\"5432:5432\"");
    assertThat(compose).contains("\"6379:6379\"");
    assertThat(compose).contains("\"5672:5672\"");
    assertThat(compose).contains("\"15672:15672\"");
  }

  @Test
  void shouldUsePersistentVolumes() throws IOException {
    String compose = Files.readString(DOCKER_COMPOSE_PATH);

    assertThat(compose).contains("postgres_data:");
    assertThat(compose).contains("redis_data:");
    assertThat(compose).contains("rabbitmq_data:");
  }

  @Test
  void shouldConfigureHealthChecksForAllServices() throws IOException {
    String compose = Files.readString(DOCKER_COMPOSE_PATH);

    assertThat(compose).contains("healthcheck:");
    assertThat(compose).contains("pg_isready");
    assertThat(compose).contains("redis-cli");
    assertThat(compose).contains("rabbitmq-diagnostics");
  }
}
