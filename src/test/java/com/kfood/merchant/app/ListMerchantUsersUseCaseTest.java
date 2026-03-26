package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListMerchantUsersUseCaseTest {

  private final IdentityUserRepository identityUserRepository = mock(IdentityUserRepository.class);
  private final MerchantTenantAccessService merchantTenantAccessService =
      mock(MerchantTenantAccessService.class);
  private final ListMerchantUsersUseCase useCase =
      new ListMerchantUsersUseCase(identityUserRepository, merchantTenantAccessService);

  @Test
  void shouldListUsersFromCorrectTenant() {
    var storeId = UUID.randomUUID();
    when(merchantTenantAccessService.getRequiredStoreId()).thenReturn(storeId);
    when(identityUserRepository.findAllByStoreIdOrderByEmailAsc(storeId))
        .thenReturn(
            List.of(
                user(storeId, "a@kfood.local", Set.of(UserRoleName.ATTENDANT)),
                user(storeId, "b@kfood.local", Set.of(UserRoleName.MANAGER))));

    var response = useCase.execute();

    assertThat(response).hasSize(2);
    assertThat(response).extracting(item -> item.email()).containsExactly("a@kfood.local", "b@kfood.local");
    verify(identityUserRepository).findAllByStoreIdOrderByEmailAsc(storeId);
  }

  @Test
  void shouldReturnEmptyListWhenTenantHasNoUsers() {
    var storeId = UUID.randomUUID();
    when(merchantTenantAccessService.getRequiredStoreId()).thenReturn(storeId);
    when(identityUserRepository.findAllByStoreIdOrderByEmailAsc(storeId)).thenReturn(List.of());

    var response = useCase.execute();

    assertThat(response).isEmpty();
  }

  @Test
  void shouldNotReturnUsersFromAnotherTenant() {
    var tenantStoreId = UUID.randomUUID();
    when(merchantTenantAccessService.getRequiredStoreId()).thenReturn(tenantStoreId);
    when(identityUserRepository.findAllByStoreIdOrderByEmailAsc(tenantStoreId))
        .thenReturn(List.of(user(tenantStoreId, "tenant@kfood.local", Set.of(UserRoleName.MANAGER))));

    var response = useCase.execute();

    assertThat(response).hasSize(1);
    assertThat(response.getFirst().email()).isEqualTo("tenant@kfood.local");
    verify(identityUserRepository).findAllByStoreIdOrderByEmailAsc(tenantStoreId);
  }

  private IdentityUserEntity user(UUID storeId, String email, Set<UserRoleName> roles) {
    var user =
        new IdentityUserEntity(UUID.randomUUID(), storeId, email, "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(roles);
    try {
      var createdAt = user.getClass().getSuperclass().getDeclaredField("createdAt");
      createdAt.setAccessible(true);
      createdAt.set(user, Instant.parse("2026-03-26T12:00:00Z"));
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
    return user;
  }
}
