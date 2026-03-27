package com.kfood.merchant.infra.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.identity.app.CreateUserService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MerchantUserManagementAdapterTest {

  private final CreateUserService createUserService = mock(CreateUserService.class);
  private final IdentityUserRepository identityUserRepository = mock(IdentityUserRepository.class);
  private final MerchantUserManagementAdapter adapter =
      new MerchantUserManagementAdapter(createUserService, identityUserRepository);

  @Test
  void shouldCreateUserThroughPortAdapter() {
    var storeId = UUID.randomUUID();
    var user = user(storeId, "manager@kfood.local", Set.of(UserRoleName.MANAGER));

    when(createUserService.create(
            storeId, "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER)))
        .thenReturn(user);

    var output =
        adapter.create(storeId, "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER));

    assertThat(output.email()).isEqualTo("manager@kfood.local");
    assertThat(output.roles()).containsExactly("MANAGER");
  }

  @Test
  void shouldListUsersThroughPortAdapter() {
    var storeId = UUID.randomUUID();
    when(identityUserRepository.findAllByStoreIdOrderByEmailAsc(storeId))
        .thenReturn(
            List.of(
                user(storeId, "a@kfood.local", Set.of(UserRoleName.ATTENDANT)),
                user(storeId, "b@kfood.local", Set.of(UserRoleName.MANAGER))));

    var outputs = adapter.listByStoreId(storeId);

    assertThat(outputs).hasSize(2);
    assertThat(outputs)
        .extracting(item -> item.email())
        .containsExactly("a@kfood.local", "b@kfood.local");
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
