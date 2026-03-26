package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.identity.app.CreateUserService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.merchant.api.CreateMerchantUserRequest;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateMerchantUserUseCaseTest {

  private final CreateUserService createUserService = mock(CreateUserService.class);
  private final MerchantTenantAccessService merchantTenantAccessService =
      mock(MerchantTenantAccessService.class);
  private final CreateMerchantUserUseCase useCase =
      new CreateMerchantUserUseCase(createUserService, merchantTenantAccessService);

  @Test
  void shouldCreateMerchantUserSuccessfully() {
    var storeId = UUID.randomUUID();
    var request =
        new CreateMerchantUserRequest(
            "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER));
    var created = user(storeId, "manager@kfood.local", Set.of(UserRoleName.MANAGER));

    when(merchantTenantAccessService.getRequiredStoreId()).thenReturn(storeId);
    when(createUserService.create(
            storeId, "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER)))
        .thenReturn(created);

    var response = useCase.execute(request);

    assertThat(response.id()).isEqualTo(created.getId());
    assertThat(response.email()).isEqualTo("manager@kfood.local");
    assertThat(response.roles()).containsExactly("MANAGER");
    assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void shouldRejectInvalidRole() {
    var request =
        new CreateMerchantUserRequest("owner@kfood.local", "Senha@123", Set.of(UserRoleName.OWNER));

    assertThatThrownBy(() -> useCase.execute(request))
        .isInstanceOf(InvalidMerchantUserRoleException.class)
        .hasMessageContaining("OWNER");
  }

  @Test
  void shouldBlockCrossTenantAttempt() {
    var request =
        new CreateMerchantUserRequest(
            "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER));

    when(merchantTenantAccessService.getRequiredStoreId()).thenThrow(new TenantAccessDeniedException());

    assertThatThrownBy(() -> useCase.execute(request))
        .isInstanceOf(TenantAccessDeniedException.class)
        .hasMessageContaining("another tenant");
  }

  @Test
  void shouldReturnSanitizedResponseWithoutSensitiveFields() {
    var storeId = UUID.randomUUID();
    var request =
        new CreateMerchantUserRequest(
            "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT));
    var created = user(storeId, "attendant@kfood.local", Set.of(UserRoleName.ATTENDANT));

    when(merchantTenantAccessService.getRequiredStoreId()).thenReturn(storeId);
    when(createUserService.create(
            storeId, "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT)))
        .thenReturn(created);

    var response = useCase.execute(request);

    assertThat(response.email()).isEqualTo("attendant@kfood.local");
    assertThat(response.roles()).containsExactly("ATTENDANT");
    assertThat(java.util.Arrays.stream(response.getClass().getRecordComponents())
            .map(RecordComponent::getName))
        .doesNotContain("password", "passwordHash", "storeId");
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
