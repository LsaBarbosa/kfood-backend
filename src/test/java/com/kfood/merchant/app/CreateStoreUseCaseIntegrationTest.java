package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.identity.persistence.IdentityUserRoleEntity;
import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.adapter.JpaMerchantCommandAdapter;
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
    assertThat(persistedStore.getCreatedAt())
        .isEqualTo(output.createdAt().truncatedTo(ChronoUnit.MICROS));

    assertThat(persistedOwner.getStoreId()).isEqualTo(persistedStore.getId());
    assertThat(persistedOwner.getRoles())
        .extracting(IdentityUserRoleEntity::getRoleName)
        .containsExactly(UserRoleName.OWNER);
    assertThat(persistedOwner.getRoles())
        .extracting(IdentityUserRoleEntity::getStoreId)
        .containsOnly(persistedStore.getId());
  }

  private IdentityUserEntity ownerWithoutStore(String email) {
    var owner =
        new IdentityUserEntity(UUID.randomUUID(), null, email, "$2a$10$hash", UserStatus.ACTIVE);
    owner.replaceRoles(Set.of(UserRoleName.OWNER));
    return owner;
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
