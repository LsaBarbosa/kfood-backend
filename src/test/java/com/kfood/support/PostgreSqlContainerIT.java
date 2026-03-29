package com.kfood.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@EnabledIfDockerAvailable
public abstract class PostgreSqlContainerIT {

  static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("kfood_test")
          .withUsername("test")
          .withPassword("test");

  private static synchronized void startContainerIfNecessary() {
    if (!POSTGRESQL_CONTAINER.isRunning()) {
      POSTGRESQL_CONTAINER.start();
    }
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () -> {
          startContainerIfNecessary();
          return POSTGRESQL_CONTAINER.getJdbcUrl();
        });
    registry.add(
        "spring.datasource.username",
        () -> {
          startContainerIfNecessary();
          return POSTGRESQL_CONTAINER.getUsername();
        });
    registry.add(
        "spring.datasource.password",
        () -> {
          startContainerIfNecessary();
          return POSTGRESQL_CONTAINER.getPassword();
        });
    registry.add(
        "spring.datasource.driver-class-name",
        () -> {
          startContainerIfNecessary();
          return POSTGRESQL_CONTAINER.getDriverClassName();
        });

    registry.add(
        "spring.flyway.url",
        () -> {
          startContainerIfNecessary();
          return POSTGRESQL_CONTAINER.getJdbcUrl();
        });
    registry.add(
        "spring.flyway.user",
        () -> {
          startContainerIfNecessary();
          return POSTGRESQL_CONTAINER.getUsername();
        });
    registry.add(
        "spring.flyway.password",
        () -> {
          startContainerIfNecessary();
          return POSTGRESQL_CONTAINER.getPassword();
        });
  }
}
