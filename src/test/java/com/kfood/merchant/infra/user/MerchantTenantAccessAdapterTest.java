package com.kfood.merchant.infra.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.app.AuthenticatedUserNotFoundException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.TenantAccessDeniedException;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MerchantTenantAccessAdapterTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final IdentityUserRepository identityUserRepository = mock(IdentityUserRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final MerchantTenantAccessAdapter service =
      new MerchantTenantAccessAdapter(
          storeRepository,
          identityUserRepository,
          currentTenantProvider,
          currentAuthenticatedUserProvider);

  @Test
  void shouldReturnStoreIdForAuthenticatedTenantUser() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store(storeId)));
    when(identityUserRepository.findById(userId)).thenReturn(Optional.of(user(userId, storeId)));

    assertThat(service.getRequiredStoreId()).isEqualTo(storeId);
  }

  @Test
  void shouldThrowWhenAuthenticatedUserBelongsToAnotherTenant() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store(storeId)));
    when(identityUserRepository.findById(userId))
        .thenReturn(Optional.of(user(userId, UUID.randomUUID())));

    assertThatThrownBy(service::getRequiredStoreId)
        .isInstanceOf(TenantAccessDeniedException.class)
        .hasMessageContaining("another tenant");
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(service::getRequiredStoreId)
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldThrowWhenAuthenticatedUserDoesNotExist() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store(storeId)));
    when(identityUserRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(service::getRequiredStoreId)
        .isInstanceOf(AuthenticatedUserNotFoundException.class)
        .hasMessageContaining(userId.toString());
  }

  private Store store(UUID storeId) {
    return new Store(
        storeId,
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }

  private IdentityUserEntity user(UUID userId, UUID storeId) {
    var user = new IdentityUserEntity(userId, storeId, "user@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.MANAGER));
    return user;
  }
}
