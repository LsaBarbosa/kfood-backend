package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.identity.persistence.IdentityUserRoleEntity;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.adapter.JpaMerchantActivationRequirementsAdapter;
import com.kfood.merchant.infra.adapter.JpaMerchantCommandAdapter;
import com.kfood.merchant.infra.adapter.JpaMerchantQueryAdapter;
import com.kfood.merchant.infra.audit.JpaMerchantStoreAuditAdapter;
import com.kfood.merchant.infra.persistence.MerchantStoreAuditEventRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.support.PostgreSqlContainerIT;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
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
  JpaMerchantCommandAdapter.class,
  JpaMerchantQueryAdapter.class,
  JpaMerchantActivationRequirementsAdapter.class,
  JpaMerchantStoreAuditAdapter.class,
  AdminChangeStoreStatusUseCaseIntegrationTest.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class AdminChangeStoreStatusUseCaseIntegrationTest extends PostgreSqlContainerIT {

  private static final Instant FIXED_AUDIT_TIME = Instant.parse("2026-03-24T15:30:00Z");

  @Autowired private AdminChangeStoreStatusUseCase adminChangeStoreStatusUseCase;

  @Autowired private StoreRepository storeRepository;

  @Autowired private IdentityUserRepository identityUserRepository;

  @Autowired private MerchantStoreAuditEventRepository merchantStoreAuditEventRepository;

  @Autowired private EntityManager entityManager;

  @Autowired private TestCurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;

  @Test
  void shouldChangeExplicitTargetStoreInsteadOfActorStoreDuringAdminCrossTenantStatusChange() {
    var targetStore = activeStore("store-target", "45.723.174/0001-10");
    var actorStore = activeStore("store-actor", "54.550.752/0001-55");
    storeRepository.saveAndFlush(targetStore);
    storeRepository.saveAndFlush(actorStore);

    var admin = adminWithStore("admin@kfood.local", actorStore.getId());
    identityUserRepository.saveAndFlush(admin);
    currentAuthenticatedUserProvider.setCurrentUserId(admin.getId());

    var output =
        adminChangeStoreStatusUseCase.execute(
            targetStore.getId(), new ChangeStoreStatusCommand(StoreStatus.SUSPENDED));

    entityManager.flush();
    entityManager.clear();

    var persistedTargetStore = storeRepository.findById(targetStore.getId()).orElseThrow();
    var persistedActorStore = storeRepository.findById(actorStore.getId()).orElseThrow();
    var persistedAdmin = identityUserRepository.findDetailedById(admin.getId()).orElseThrow();
    var targetStoreEvents =
        merchantStoreAuditEventRepository.findAllByStoreIdOrderByOccurredAtAsc(targetStore.getId());
    var actorStoreEvents =
        merchantStoreAuditEventRepository.findAllByStoreIdOrderByOccurredAtAsc(actorStore.getId());

    assertThat(output.id()).isEqualTo(targetStore.getId());
    assertThat(output.status()).isEqualTo(StoreStatus.SUSPENDED);
    assertThat(persistedTargetStore.getStatus()).isEqualTo(StoreStatus.SUSPENDED);
    assertThat(persistedActorStore.getStatus()).isEqualTo(StoreStatus.ACTIVE);
    assertThat(persistedAdmin.getStoreId()).isEqualTo(actorStore.getId());
    assertThat(persistedAdmin.getRoles())
        .extracting(IdentityUserRoleEntity::getStoreId)
        .containsOnly(actorStore.getId());

    assertThat(targetStoreEvents)
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.getStoreId()).isEqualTo(targetStore.getId());
              assertThat(event.getActorUserId()).isEqualTo(admin.getId());
              assertThat(event.getEntityId()).isEqualTo(targetStore.getId());
              assertThat(event.getBeforeStatus()).isEqualTo(StoreStatus.ACTIVE);
              assertThat(event.getAfterStatus()).isEqualTo(StoreStatus.SUSPENDED);
              assertThat(event.getOccurredAt()).isEqualTo(FIXED_AUDIT_TIME);
            });
    assertThat(actorStoreEvents).isEmpty();
  }

  private Store activeStore(String slug, String cnpj) {
    var store = new Store(UUID.randomUUID(), "Loja Base", slug, cnpj, "21999990000", "UTC");
    store.activate();
    return store;
  }

  private IdentityUserEntity adminWithStore(String email, UUID storeId) {
    var admin =
        new IdentityUserEntity(UUID.randomUUID(), storeId, email, "$2a$10$hash", UserStatus.ACTIVE);
    admin.replaceRoles(Set.of(UserRoleName.ADMIN));
    return admin;
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(FIXED_AUDIT_TIME, ZoneOffset.UTC);
    }

    @Bean
    TestCurrentAuthenticatedUserProvider currentAuthenticatedUserProvider() {
      return new TestCurrentAuthenticatedUserProvider();
    }

    @Bean
    StoreActivationRequirementsService storeActivationRequirementsService(
        JpaMerchantActivationRequirementsAdapter merchantActivationRequirementsPort) {
      return new StoreActivationRequirementsService(merchantActivationRequirementsPort);
    }

    @Bean
    AdminChangeStoreStatusUseCase adminChangeStoreStatusUseCase(
        JpaMerchantCommandAdapter merchantCommandPort,
        JpaMerchantQueryAdapter merchantQueryPort,
        JpaMerchantStoreAuditAdapter merchantStoreAuditPort,
        CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider,
        StoreActivationRequirementsService storeActivationRequirementsService) {
      return new AdminChangeStoreStatusUseCase(
          merchantCommandPort,
          merchantQueryPort,
          merchantStoreAuditPort,
          currentAuthenticatedUserProvider,
          storeActivationRequirementsService);
    }
  }

  static class TestCurrentAuthenticatedUserProvider implements CurrentAuthenticatedUserProvider {

    private UUID currentUserId;

    void setCurrentUserId(UUID currentUserId) {
      this.currentUserId = currentUserId;
    }

    @Override
    public UUID getRequiredUserId() {
      return currentUserId;
    }
  }
}
