package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.identity.persistence.IdentityUserRoleEntity;
import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.adapter.JpaMerchantCommandAdapter;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.support.PostgreSqlContainerIT;
import jakarta.persistence.EntityManager;
import java.time.temporal.ChronoUnit;
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
  JpaMerchantCommandAdapter.class,
  CreateStoreUseCaseIntegrationTest.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class CreateStoreUseCaseIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private CreateStoreUseCase createStoreUseCase;

  @Autowired private MerchantCommandPort merchantCommandPort;

  @Autowired private StoreRepository storeRepository;

  @Autowired private IdentityUserRepository identityUserRepository;

  @Autowired private EntityManager entityManager;

  @Autowired private TestCurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;

  @Test
  @DisplayName("should bind owner to newly created store using real create store flow")
  void shouldBindOwnerToNewlyCreatedStoreUsingRealCreateStoreFlow() {
    var owner = ownerWithoutStore("owner@kfood.local");
    identityUserRepository.saveAndFlush(owner);
    currentAuthenticatedUserProvider.setCurrentUserId(owner.getId());

    var output =
        createStoreUseCase.execute(
            new CreateStoreCommand(
                "Nova Loja",
                "nova-loja",
                "45.723.174/0001-10",
                "21999990000",
                "America/Sao_Paulo"));

    entityManager.flush();
    entityManager.clear();

    assertThat(merchantCommandPort).isInstanceOf(JpaMerchantCommandAdapter.class);

    var persistedStore = storeRepository.findById(output.id()).orElseThrow();
    var persistedOwner = identityUserRepository.findDetailedById(owner.getId()).orElseThrow();

    assertThat(persistedStore.getId()).isEqualTo(output.id());
    assertThat(persistedStore.getSlug()).isEqualTo("nova-loja");
    assertThat(persistedStore.getStatus()).isEqualTo(StoreStatus.SETUP);
    assertThat(persistedStore.getCreatedAt()).isNotNull();
    assertThat(
            Math.abs(ChronoUnit.MICROS.between(output.createdAt(), persistedStore.getCreatedAt())))
        .isLessThanOrEqualTo(1);

    assertThat(persistedOwner.getStoreId()).isEqualTo(persistedStore.getId());
    assertThat(persistedOwner.getRoles())
        .extracting(IdentityUserRoleEntity::getRoleName)
        .containsExactly(UserRoleName.OWNER);
    assertThat(persistedOwner.getRoles())
        .extracting(IdentityUserRoleEntity::getStoreId)
        .containsOnly(persistedStore.getId());
  }

  @Test
  @DisplayName("should keep original binding when owner already belongs to another store")
  void shouldKeepOriginalBindingWhenOwnerAlreadyBelongsToAnotherStore() {
    var originalStore = store("loja-original", "45.723.174/0001-10");
    storeRepository.saveAndFlush(originalStore);

    var owner = ownerWithStore("owner-vinculado@kfood.local", originalStore.getId());
    identityUserRepository.saveAndFlush(owner);
    currentAuthenticatedUserProvider.setCurrentUserId(owner.getId());

    entityManager.clear();

    assertThatThrownBy(
            () ->
                createStoreUseCase.execute(
                    new CreateStoreCommand(
                        "Segunda Loja",
                        "segunda-loja",
                        "54.550.752/0001-55",
                        "21988887777",
                        "America/Sao_Paulo")))
        .isInstanceOf(OwnerAlreadyBoundToAnotherStoreException.class)
        .hasMessageContaining(originalStore.getId().toString());

    entityManager.clear();

    assertThat(merchantCommandPort).isInstanceOf(JpaMerchantCommandAdapter.class);
    assertThat(storeRepository.count()).isEqualTo(1);
    assertThat(storeRepository.existsBySlug("segunda-loja")).isFalse();

    var persistedOriginalStore = storeRepository.findById(originalStore.getId()).orElseThrow();
    var persistedOwner = identityUserRepository.findDetailedById(owner.getId()).orElseThrow();

    assertThat(persistedOriginalStore.getSlug()).isEqualTo("loja-original");
    assertThat(persistedOwner.getStoreId()).isEqualTo(originalStore.getId());
    assertThat(persistedOwner.getRoles())
        .extracting(IdentityUserRoleEntity::getRoleName)
        .containsExactly(UserRoleName.OWNER);
    assertThat(persistedOwner.getRoles())
        .extracting(IdentityUserRoleEntity::getStoreId)
        .containsOnly(originalStore.getId());
  }

  @Test
  @DisplayName("should allow admin to create store without binding admin to created store")
  void shouldAllowAdminToCreateStoreWithoutBindingAdminToCreatedStore() {
    var admin = adminWithoutStore("admin@kfood.local");
    identityUserRepository.saveAndFlush(admin);
    currentAuthenticatedUserProvider.setCurrentUserId(admin.getId());

    var output =
        createStoreUseCase.execute(
            new CreateStoreCommand(
                "Loja Admin",
                "loja-admin",
                "54.550.752/0001-55",
                "21977776666",
                "America/Sao_Paulo"));

    entityManager.flush();
    entityManager.clear();

    assertThat(merchantCommandPort).isInstanceOf(JpaMerchantCommandAdapter.class);

    var persistedStore = storeRepository.findById(output.id()).orElseThrow();
    var persistedAdmin = identityUserRepository.findDetailedById(admin.getId()).orElseThrow();

    assertThat(persistedStore.getId()).isEqualTo(output.id());
    assertThat(persistedStore.getSlug()).isEqualTo("loja-admin");
    assertThat(persistedStore.getStatus()).isEqualTo(StoreStatus.SETUP);
    assertThat(persistedAdmin.getStoreId()).isNull();
    assertThat(persistedAdmin.getRoles())
        .extracting(IdentityUserRoleEntity::getRoleName)
        .containsExactly(UserRoleName.ADMIN);
    assertThat(persistedAdmin.getRoles())
        .extracting(IdentityUserRoleEntity::getStoreId)
        .containsOnlyNulls();
  }

  private IdentityUserEntity ownerWithoutStore(String email) {
    var owner =
        new IdentityUserEntity(UUID.randomUUID(), null, email, "$2a$10$hash", UserStatus.ACTIVE);
    owner.replaceRoles(Set.of(UserRoleName.OWNER));
    return owner;
  }

  private IdentityUserEntity adminWithoutStore(String email) {
    var admin =
        new IdentityUserEntity(UUID.randomUUID(), null, email, "$2a$10$hash", UserStatus.ACTIVE);
    admin.replaceRoles(Set.of(UserRoleName.ADMIN));
    return admin;
  }

  private IdentityUserEntity ownerWithStore(String email, UUID storeId) {
    var owner =
        new IdentityUserEntity(UUID.randomUUID(), storeId, email, "$2a$10$hash", UserStatus.ACTIVE);
    owner.replaceRoles(Set.of(UserRoleName.OWNER));
    return owner;
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja Base", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    TestCurrentAuthenticatedUserProvider currentAuthenticatedUserProvider() {
      return new TestCurrentAuthenticatedUserProvider();
    }

    @Bean
    CreateStoreUseCase createStoreUseCase(
        MerchantCommandPort merchantCommandPort,
        CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider) {
      return new CreateStoreUseCase(merchantCommandPort, currentAuthenticatedUserProvider);
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
