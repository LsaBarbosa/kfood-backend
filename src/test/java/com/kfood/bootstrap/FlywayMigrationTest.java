package com.kfood.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

class FlywayMigrationTest {

  private DataSource dataSource;

  @BeforeEach
  void setUp() {
    dataSource =
        DataSourceBuilder.create()
            .url(
                "jdbc:h2:mem:flyway_migration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
            .username("sa")
            .password("")
            .driverClassName("org.h2.Driver")
            .build();

    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
  }

  @Test
  void shouldApplyBaselineMigrationOnStartup() throws Exception {
    assertThat(tableExists("flyway_schema_history")).isTrue();
    assertThat(tableExists("store")).isTrue();
    assertThat(tableExists("identity_user")).isTrue();
    assertThat(tableExists("identity_user_role")).isTrue();
    assertThat(tableExists("store_hours")).isTrue();
    assertThat(tableExists("delivery_zone")).isTrue();
    assertThat(tableExists("store_terms_acceptance")).isTrue();
  }

  @Test
  void shouldRegisterVersionOneInFlywayHistory() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                """
                     select count(*)
                     from flyway_schema_history
                     where version = '1'
                       and success = true
                     """)) {

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
  }

  private boolean tableExists(String tableName) throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, null)) {
      return resultSet.next();
    }
  }
}
