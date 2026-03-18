package com.kfood.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

class FlywayFailureScenariosTest {

  @Test
  void shouldFailWhenThereAreDuplicateMigrationVersions() {
    DataSource dataSource =
        DataSourceBuilder.create()
            .url(
                "jdbc:h2:mem:duplicate_migration_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
            .username("sa")
            .password("")
            .driverClassName("org.h2.Driver")
            .build();

    Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/test-migration/duplicate")
            .load();

    assertThatThrownBy(flyway::migrate)
        .hasMessageContaining("Found more than one migration with version 1");
  }

  @Test
  void shouldFailWhenRepeatableOrVersionOrderingIsInvalidForConfiguredSet() {
    DataSource dataSource =
        DataSourceBuilder.create()
            .url(
                "jdbc:h2:mem:invalid_order_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
            .username("sa")
            .password("")
            .driverClassName("org.h2.Driver")
            .build();

    Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .outOfOrder(false)
            .locations("classpath:db/test-migration/out-of-order")
            .load();

    flyway.migrate();

    Flyway secondFlyway =
        Flyway.configure()
            .dataSource(dataSource)
            .outOfOrder(false)
            .locations("classpath:db/test-migration/out-of-order-with-gap")
            .load();

    assertThatThrownBy(secondFlyway::migrate)
        .isInstanceOf(Exception.class)
        .hasMessageContaining("Detected resolved migration not applied to database: 1");
  }
}
