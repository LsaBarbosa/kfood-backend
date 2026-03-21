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
    assertThat(tableExists("catalog_category")).isTrue();
    assertThat(tableExists("catalog_product")).isTrue();
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

  @Test
  void shouldRegisterVersionTwoInFlywayHistory() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                """
                     select count(*)
                     from flyway_schema_history
                     where version = '2'
                       and success = true
                     """)) {

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void shouldRegisterVersionThreeInFlywayHistory() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                """
                     select count(*)
                     from flyway_schema_history
                     where version = '3'
                       and success = true
                     """)) {

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void shouldRegisterVersionFourInFlywayHistory() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                """
                     select count(*)
                     from flyway_schema_history
                     where version = '4'
                       and success = true
                     """)) {

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void shouldRegisterVersionFiveInFlywayHistory() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                """
                     select count(*)
                     from flyway_schema_history
                     where version = '5'
                       and success = true
                     """)) {

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void shouldRegisterVersionSixInFlywayHistory() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                """
                     select count(*)
                     from flyway_schema_history
                     where version = '6'
                       and success = true
                     """)) {

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void shouldRegisterVersionSevenInFlywayHistory() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                """
                     select count(*)
                     from flyway_schema_history
                     where version = '7'
                       and success = true
                     """)) {

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void shouldRegisterVersionEightInFlywayHistory() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                """
                     select count(*)
                     from flyway_schema_history
                     where version = '8'
                       and success = true
                     """)) {

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void shouldApplySetupAsStoreDefaultStatusAfterApplyingMigrations() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          insert into store (id, slug, name, cnpj, phone, timezone, created_at, updated_at)
          values ('33333333-3333-3333-3333-333333333333',
                  'store-default-status',
                  'Loja Default',
                  '45.723.174/0001-10',
                  '21999990000',
                  'America/Sao_Paulo',
                  current_timestamp,
                  current_timestamp)
          """);

      try (ResultSet resultSet =
          statement.executeQuery(
              """
              select status
              from store
              where id = '33333333-3333-3333-3333-333333333333'
              """)) {
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getString(1)).isEqualTo("SETUP");
      }
    }
  }

  @Test
  void shouldRejectInvalidIdentityRoleAfterApplyingMigrations() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          insert into identity_user (id, store_id, email, password_hash, status, created_at, updated_at)
          values ('11111111-1111-1111-1111-111111111111', null, 'flyway-role@kfood.local', 'hash', 'ACTIVE',
                  current_timestamp, current_timestamp)
          """);

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  statement.executeUpdate(
                      """
                      insert into identity_user_role (id, user_id, store_id, role_name, created_at)
                      values ('22222222-2222-2222-2222-222222222222',
                              '11111111-1111-1111-1111-111111111111',
                              null,
                              'SUPER_ADMIN',
                              current_timestamp)
                      """))
          .hasMessageContaining("chk_identity_user_role_name");
    }
  }

  @Test
  void shouldApplyStoreHoursConstraintsAndHoursVersionAfterApplyingMigrations() throws Exception {
    assertThat(columnExists("store", "hours_version")).isTrue();

    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          insert into store (id, slug, name, cnpj, phone, timezone, created_at, updated_at)
          values ('44444444-4444-4444-4444-444444444444',
                  'store-hours-constraint',
                  'Loja Horario',
                  '45.723.174/0001-10',
                  '21999990000',
                  'America/Sao_Paulo',
                  current_timestamp,
                  current_timestamp)
          """);

      statement.executeUpdate(
          """
          insert into store_hours (id, store_id, day_of_week, open_time, close_time, is_closed, created_at, updated_at)
          values ('55555555-5555-5555-5555-555555555555',
                  '44444444-4444-4444-4444-444444444444',
                  'MONDAY',
                  '10:00:00',
                  '22:00:00',
                  false,
                  current_timestamp,
                  current_timestamp)
          """);

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  statement.executeUpdate(
                      """
                      insert into store_hours (id, store_id, day_of_week, open_time, close_time, is_closed, created_at, updated_at)
                      values ('66666666-6666-6666-6666-666666666666',
                              '44444444-4444-4444-4444-444444444444',
                              'MONDAY',
                              '09:00:00',
                              '21:00:00',
                              false,
                              current_timestamp,
                              current_timestamp)
                      """))
          .isInstanceOf(Exception.class);

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  statement.executeUpdate(
                      """
                      insert into store_hours (id, store_id, day_of_week, open_time, close_time, is_closed, created_at, updated_at)
                      values ('77777777-7777-7777-7777-777777777777',
                              '44444444-4444-4444-4444-444444444444',
                              'TUESDAY',
                              '22:00:00',
                              '10:00:00',
                              false,
                              current_timestamp,
                              current_timestamp)
                      """))
          .isInstanceOf(Exception.class);
    }
  }

  @Test
  void shouldApplyDeliveryZoneConstraintsAfterApplyingMigrations() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          insert into store (id, slug, name, cnpj, phone, timezone, created_at, updated_at)
          values ('88888888-8888-8888-8888-888888888888',
                  'store-zone-constraint',
                  'Loja Zona',
                  '45.723.174/0001-10',
                  '21999990000',
                  'America/Sao_Paulo',
                  current_timestamp,
                  current_timestamp)
          """);

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  statement.executeUpdate(
                      """
                      insert into delivery_zone (id, store_id, zone_name, fee_amount, min_order_amount, active, created_at, updated_at)
                      values ('99999999-9999-9999-9999-999999999999',
                              '88888888-8888-8888-8888-888888888888',
                              'Centro',
                              -1.00,
                              25.00,
                              true,
                              current_timestamp,
                              current_timestamp)
                      """))
          .isInstanceOf(Exception.class);
    }
  }

  @Test
  void shouldApplyPerformanceIndexesAfterApplyingMigrations() throws Exception {
    assertThat(indexExists("store_terms_acceptance", "idx_store_terms_acceptance_store_doc"))
        .isTrue();
    assertThat(
            indexExists("store_terms_acceptance", "idx_store_terms_acceptance_store_accepted_at"))
        .isTrue();
    assertThat(indexExists("delivery_zone", "idx_delivery_zone_store_active_zone_name")).isTrue();
  }

  @Test
  void shouldApplyCatalogCategoryMigrationAfterApplyingMigrations() throws Exception {
    assertThat(indexExists("catalog_category", "idx_catalog_category_store_active_sort_order"))
        .isTrue();

    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          insert into store (id, slug, name, cnpj, phone, timezone, created_at, updated_at)
          values ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
                  'store-category-constraint',
                  'Loja Categoria',
                  '45.723.174/0001-10',
                  '21999990000',
                  'America/Sao_Paulo',
                  current_timestamp,
                  current_timestamp)
          """);

      statement.executeUpdate(
          """
          insert into catalog_category (id, store_id, name, sort_order, active, created_at, updated_at)
          values ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
                  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
                  'Pizzas',
                  10,
                  true,
                  current_timestamp,
                  current_timestamp)
          """);

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  statement.executeUpdate(
                      """
                      insert into catalog_category (id, store_id, name, sort_order, active, created_at, updated_at)
                      values ('cccccccc-cccc-cccc-cccc-cccccccccccc',
                              'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
                              'Bebidas',
                              -1,
                              true,
                              current_timestamp,
                              current_timestamp)
                      """))
          .isInstanceOf(Exception.class);

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  statement.executeUpdate(
                      """
                      insert into catalog_category (id, store_id, name, sort_order, active, created_at, updated_at)
                      values ('dddddddd-dddd-dddd-dddd-dddddddddddd',
                              'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
                              'Pizzas',
                              20,
                              true,
                              current_timestamp,
                              current_timestamp)
                      """))
          .isInstanceOf(Exception.class);
    }
  }

  @Test
  void shouldApplyCatalogProductMigrationAfterApplyingMigrations() throws Exception {
    assertThat(indexExists("catalog_product", "idx_catalog_product_store_active_paused_sort_order"))
        .isTrue();
    assertThat(indexExists("catalog_product", "idx_catalog_product_category_id")).isTrue();

    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          insert into store (id, slug, name, cnpj, phone, timezone, created_at, updated_at)
          values ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
                  'store-product-constraint',
                  'Loja Produto',
                  '45.723.174/0001-10',
                  '21999990000',
                  'America/Sao_Paulo',
                  current_timestamp,
                  current_timestamp)
          """);

      statement.executeUpdate(
          """
          insert into catalog_category (id, store_id, name, sort_order, active, created_at, updated_at)
          values ('ffffffff-ffff-ffff-ffff-ffffffffffff',
                  'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
                  'Pizzas',
                  10,
                  true,
                  current_timestamp,
                  current_timestamp)
          """);

      statement.executeUpdate(
          """
          insert into catalog_product (id, store_id, category_id, name, description, base_price, image_url, sort_order, active, paused, created_at, updated_at)
          values ('11111111-2222-3333-4444-555555555555',
                  'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
                  'ffffffff-ffff-ffff-ffff-ffffffffffff',
                  'Pizza Calabresa',
                  'Pizza com calabresa e cebola',
                  39.90,
                  null,
                  20,
                  true,
                  false,
                  current_timestamp,
                  current_timestamp)
          """);

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  statement.executeUpdate(
                      """
                      insert into catalog_product (id, store_id, category_id, name, description, base_price, image_url, sort_order, active, paused, created_at, updated_at)
                      values ('66666666-7777-8888-9999-000000000000',
                              'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
                              'ffffffff-ffff-ffff-ffff-ffffffffffff',
                              'Pizza Negativa',
                              'Pizza com preco invalido',
                              -1.00,
                              null,
                              30,
                              true,
                              false,
                              current_timestamp,
                              current_timestamp)
                      """))
          .isInstanceOf(Exception.class);
    }
  }

  private boolean tableExists(String tableName) throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, null)) {
      return resultSet.next();
    }
  }

  private boolean columnExists(String tableName, String columnName) throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet resultSet =
            connection.getMetaData().getColumns(null, null, tableName, columnName)) {
      return resultSet.next();
    }
  }

  private boolean indexExists(String tableName, String indexName) throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet resultSet =
            connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
      while (resultSet.next()) {
        var currentIndexName = resultSet.getString("INDEX_NAME");
        if (currentIndexName != null && currentIndexName.equalsIgnoreCase(indexName)) {
          return true;
        }
      }
      return false;
    }
  }
}
