package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class StoreTermsAcceptanceRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private StoreRepository storeRepository;

  @Autowired private IdentityUserRepository identityUserRepository;

  @Autowired private StoreTermsAcceptanceRepository storeTermsAcceptanceRepository;

  @Test
  @DisplayName("should persist acceptance history and query most recent first")
  void shouldPersistAcceptanceHistoryAndQueryMostRecentFirst() {
    var store =
        storeRepository.saveAndFlush(
            new Store(
                UUID.randomUUID(),
                "Loja do Bairro",
                "loja-do-bairro",
                "45.723.174/0001-10",
                "21999990000",
                "America/Sao_Paulo"));

    var owner =
        new IdentityUserEntity(
            UUID.randomUUID(),
            store.getId(),
            "owner@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    owner.replaceRoles(Set.of(UserRoleName.OWNER));
    owner = identityUserRepository.saveAndFlush(owner);

    storeTermsAcceptanceRepository.saveAndFlush(
        new StoreTermsAcceptance(
            UUID.randomUUID(),
            store.getId(),
            owner.getId(),
            LegalDocumentType.TERMS_OF_USE,
            "2026.03",
            Instant.parse("2026-03-20T13:15:00Z"),
            "203.0.113.9"));

    storeTermsAcceptanceRepository.saveAndFlush(
        new StoreTermsAcceptance(
            UUID.randomUUID(),
            store.getId(),
            owner.getId(),
            LegalDocumentType.TERMS_OF_USE,
            "2026.04",
            Instant.parse("2026-04-20T13:15:00Z"),
            "203.0.113.10"));

    var history =
        storeTermsAcceptanceRepository.findAllByStoreIdOrderByAcceptedAtDesc(store.getId());

    assertThat(history).hasSize(2);
    assertThat(history.getFirst().getDocumentVersion()).isEqualTo("2026.04");
    assertThat(history.getFirst().getRequestIp()).isEqualTo("203.0.113.10");
    assertThat(history.getLast().getDocumentVersion()).isEqualTo("2026.03");
    assertThat(storeTermsAcceptanceRepository.existsByStoreId(store.getId())).isTrue();
  }
}
