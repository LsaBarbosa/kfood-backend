package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.identity.persistence.IdentityUserRoleEntity;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateStoreUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final IdentityUserRepository identityUserRepository = mock(IdentityUserRepository.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final CreateStoreUseCase createStoreUseCase =
      new CreateStoreUseCase(
          storeRepository, identityUserRepository, currentAuthenticatedUserProvider);

  @Test
  void shouldCreateStoreAndBindOwnerToNewStore() throws Exception {
    var userId = UUID.randomUUID();
    var request =
        new CreateStoreCommand(
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var owner = ownerUser(userId, null);

    var persistedStore =
        new Store(
            UUID.randomUUID(),
            request.name(),
            request.slug(),
            request.cnpj(),
            request.phone(),
            request.timezone());
    setAuditableField(persistedStore, "createdAt", Instant.parse("2026-03-20T10:00:00Z"));

    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(identityUserRepository.findDetailedById(userId)).thenReturn(Optional.of(owner));
    when(storeRepository.existsBySlug(request.slug())).thenReturn(false);
    when(storeRepository.saveAndFlush(any(Store.class))).thenReturn(persistedStore);
    when(identityUserRepository.saveAndFlush(owner)).thenReturn(owner);

    var response = createStoreUseCase.execute(request);

    assertThat(response.id()).isEqualTo(persistedStore.getId());
    assertThat(response.slug()).isEqualTo("loja-do-bairro");
    assertThat(response.status()).isEqualTo(StoreStatus.SETUP);
    assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-03-20T10:00:00Z"));
    assertThat(owner.getStoreId()).isEqualTo(persistedStore.getId());
    assertThat(owner.getRoles())
        .extracting(IdentityUserRoleEntity::getStoreId)
        .containsOnly(persistedStore.getId());
  }

  @Test
  void shouldCreateStoreWithoutBindingAdminUser() throws Exception {
    var userId = UUID.randomUUID();
    var request =
        new CreateStoreCommand(
            "Loja Admin", "loja-admin", "45.723.174/0001-10", "21999990000", "America/Sao_Paulo");
    var admin = adminUser(userId);
    var persistedStore =
        new Store(
            UUID.randomUUID(),
            request.name(),
            request.slug(),
            request.cnpj(),
            request.phone(),
            request.timezone());
    setAuditableField(persistedStore, "createdAt", Instant.parse("2026-03-20T10:00:00Z"));

    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(identityUserRepository.findDetailedById(userId)).thenReturn(Optional.of(admin));
    when(storeRepository.existsBySlug(request.slug())).thenReturn(false);
    when(storeRepository.saveAndFlush(any(Store.class))).thenReturn(persistedStore);

    var response = createStoreUseCase.execute(request);

    assertThat(response.id()).isEqualTo(persistedStore.getId());
    assertThat(admin.getStoreId()).isNull();
    verify(identityUserRepository, never()).saveAndFlush(admin);
  }

  @Test
  void shouldRejectDuplicatedSlug() {
    var userId = UUID.randomUUID();
    var request =
        new CreateStoreCommand(
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(identityUserRepository.findDetailedById(userId))
        .thenReturn(Optional.of(ownerUser(userId, null)));
    when(storeRepository.existsBySlug("loja-do-bairro")).thenReturn(true);

    assertThatThrownBy(() -> createStoreUseCase.execute(request))
        .isInstanceOf(StoreSlugAlreadyExistsException.class)
        .hasMessageContaining("loja-do-bairro");
  }

  @Test
  void shouldRejectCrossStoreBindingAttempt() {
    var userId = UUID.randomUUID();
    var currentStoreId = UUID.randomUUID();
    var request =
        new CreateStoreCommand(
            "Outra Loja", "outra-loja", "45.723.174/0001-10", "21999990000", "America/Sao_Paulo");

    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(identityUserRepository.findDetailedById(userId))
        .thenReturn(Optional.of(ownerUser(userId, currentStoreId)));

    assertThatThrownBy(() -> createStoreUseCase.execute(request))
        .isInstanceOf(OwnerAlreadyBoundToAnotherStoreException.class)
        .hasMessageContaining(currentStoreId.toString());
  }

  @Test
  void shouldRejectWhenAuthenticatedUserDoesNotExist() {
    var userId = UUID.randomUUID();
    var request =
        new CreateStoreCommand(
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(identityUserRepository.findDetailedById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> createStoreUseCase.execute(request))
        .isInstanceOf(AuthenticatedUserNotFoundException.class)
        .hasMessageContaining(userId.toString());
  }

  private IdentityUserEntity ownerUser(UUID userId, UUID storeId) {
    var user =
        new IdentityUserEntity(
            userId, storeId, "owner@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));
    return user;
  }

  private IdentityUserEntity adminUser(UUID userId) {
    var user =
        new IdentityUserEntity(userId, null, "admin@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.ADMIN));
    return user;
  }

  private void setAuditableField(Store store, String fieldName, Instant value) throws Exception {
    Field field = store.getClass().getSuperclass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(store, value);
  }
}
