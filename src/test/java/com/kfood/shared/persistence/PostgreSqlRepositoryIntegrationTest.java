package com.kfood.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.support.PostgreSqlContainerIT;
import com.kfood.support.entity.TestAuditEntity;
import com.kfood.support.repository.TestAuditEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class PostgreSqlRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private TestAuditEntityRepository repository;

  @Test
  @DisplayName("should persist and load entity using real PostgreSQL")
  void shouldPersistAndLoadEntityUsingRealPostgreSql() {
    TestAuditEntity entity = new TestAuditEntity("integration-test");

    TestAuditEntity saved = repository.saveAndFlush(entity);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    TestAuditEntity found = repository.findById(saved.getId()).orElseThrow();

    assertThat(found.getName()).isEqualTo("integration-test");
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  @Test
  @Transactional
  @Rollback
  @DisplayName("should rollback transaction and keep tests isolated")
  void shouldRollbackTransactionAndKeepTestsIsolated() {
    long countBefore = repository.count();

    repository.save(new TestAuditEntity("rollback-test"));

    long countDuringTransaction = repository.count();

    assertThat(countDuringTransaction).isEqualTo(countBefore + 1);
  }

  @Test
  @DisplayName("should start each test with clean state")
  void shouldStartEachTestWithCleanState() {
    assertThat(repository.count()).isZero();
  }
}
