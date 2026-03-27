package com.kfood.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgreSqlContainerIT {

  static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("kfood_test")
          .withUsername("test")
          .withPassword("test");

  static {
    POSTGRESQL_CONTAINER.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
    registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
    registry.add("spring.datasource.driver-class-name", POSTGRESQL_CONTAINER::getDriverClassName);

    registry.add("spring.flyway.url", POSTGRESQL_CONTAINER::getJdbcUrl);
    registry.add("spring.flyway.user", POSTGRESQL_CONTAINER::getUsername);
    registry.add("spring.flyway.password", POSTGRESQL_CONTAINER::getPassword);
  }
}
