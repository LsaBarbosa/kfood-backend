package com.kfood.merchant.infra.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.MerchantStoreAuditEventRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@Import({
  TestJpaAuditingConfig.class,
  JpaMerchantStoreAuditAdapter.class,
  JpaMerchantStoreAuditAdapterIntegrationTest.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class JpaMerchantStoreAuditAdapterIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private JpaMerchantStoreAuditAdapter auditAdapter;

  @Autowired private StoreRepository storeRepository;

  @Autowired private IdentityUserRepository identityUserRepository;

  @Autowired private MerchantStoreAuditEventRepository merchantStoreAuditEventRepository;

  @Test
  @DisplayName("should persist terms acceptance audit with minimal traceability fields")
  void shouldPersistTermsAcceptanceAuditWithMinimalTraceabilityFields() {
    var store = storeRepository.saveAndFlush(store("loja-auditoria", "45.723.174/0001-10"));
    var owner = identityUserRepository.saveAndFlush(owner(store.getId()));
    var acceptanceId = UUID.randomUUID();
    var acceptedAt = Instant.parse("2026-03-20T13:15:00Z");

    auditAdapter.recordTermsAccepted(
        store.getId(),
        owner.getId(),
        acceptanceId,
        LegalDocumentType.TERMS_OF_USE,
        "2026.03",
        acceptedAt);

    var events =
        merchantStoreAuditEventRepository.findAllByStoreIdOrderByOccurredAtAsc(store.getId());

    assertThat(events)
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.getId()).isNotNull();
              assertThat(event.getStoreId()).isEqualTo(store.getId());
              assertThat(event.getActorUserId()).isEqualTo(owner.getId());
              assertThat(event.getEventType()).isEqualTo("LEGAL_TERMS_ACCEPTED");
              assertThat(event.getEntityType()).isEqualTo("STORE_TERMS_ACCEPTANCE");
              assertThat(event.getEntityId()).isEqualTo(acceptanceId);
              assertThat(event.getOccurredAt()).isEqualTo(acceptedAt);
              assertThat(event.getDocumentType()).isEqualTo(LegalDocumentType.TERMS_OF_USE);
              assertThat(event.getDocumentVersion()).isEqualTo("2026.03");
              assertThat(event.getAcceptedAt()).isEqualTo(acceptedAt);
              assertThat(event.getBeforeStatus()).isNull();
              assertThat(event.getAfterStatus()).isNull();
            });
  }

  @Test
  @DisplayName("should persist status change audit with only status transition details")
  void shouldPersistStatusChangeAuditWithOnlyStatusTransitionDetails() {
    var store = storeRepository.saveAndFlush(store("loja-status", "54.550.752/0001-55"));
    var owner = identityUserRepository.saveAndFlush(owner(store.getId()));

    auditAdapter.recordStoreStatusChanged(
        store.getId(), owner.getId(), StoreStatus.SETUP, StoreStatus.ACTIVE);

    var events =
        merchantStoreAuditEventRepository.findAllByStoreIdOrderByOccurredAtAsc(store.getId());

    assertThat(events)
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.getId()).isNotNull();
              assertThat(event.getStoreId()).isEqualTo(store.getId());
              assertThat(event.getActorUserId()).isEqualTo(owner.getId());
              assertThat(event.getEventType()).isEqualTo("STORE_STATUS_CHANGED");
              assertThat(event.getEntityType()).isEqualTo("STORE");
              assertThat(event.getEntityId()).isEqualTo(store.getId());
              assertThat(event.getOccurredAt()).isEqualTo(Instant.parse("2026-03-21T18:45:00Z"));
              assertThat(event.getBeforeStatus()).isEqualTo(StoreStatus.SETUP);
              assertThat(event.getAfterStatus()).isEqualTo(StoreStatus.ACTIVE);
              assertThat(event.getDocumentType()).isNull();
              assertThat(event.getDocumentVersion()).isNull();
              assertThat(event.getAcceptedAt()).isNull();
            });
  }

  private Store store(String slug, String cnpj) {
    return new Store(UUID.randomUUID(), "Loja Auditoria", slug, cnpj, "21999990000", "UTC");
  }

  private IdentityUserEntity owner(UUID storeId) {
    var owner =
        new IdentityUserEntity(
            UUID.randomUUID(), storeId, "owner@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    owner.replaceRoles(Set.of(UserRoleName.OWNER));
    return owner;
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-03-21T18:45:00Z"), ZoneOffset.UTC);
    }
  }
}
