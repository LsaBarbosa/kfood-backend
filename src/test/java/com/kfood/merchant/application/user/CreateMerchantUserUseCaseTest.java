package com.kfood.merchant.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import com.kfood.merchant.app.TenantAccessDeniedException;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateMerchantUserUseCaseTest {

  private final MerchantUserManagementPort merchantUserManagementPort =
      mock(MerchantUserManagementPort.class);
  private final MerchantTenantAccessPort merchantTenantAccessPort =
      mock(MerchantTenantAccessPort.class);
  private final CreateMerchantUserUseCase useCase =
      new CreateMerchantUserUseCase(merchantUserManagementPort, merchantTenantAccessPort);

  @Test
  void shouldCreateMerchantUserSuccessfully() {
    var storeId = UUID.randomUUID();
    var command =
        new CreateMerchantUserCommand(
            "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER));
    var created =
        new MerchantUserOutput(
            UUID.randomUUID(),
            "manager@kfood.local",
            List.of("MANAGER"),
            UserStatus.ACTIVE,
            Instant.parse("2026-03-26T12:00:00Z"));

    when(merchantTenantAccessPort.getRequiredStoreId()).thenReturn(storeId);
    when(merchantUserManagementPort.create(
            storeId, "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER)))
        .thenReturn(created);

    var response = useCase.execute(command);

    assertThat(response.id()).isEqualTo(created.id());
    assertThat(response.email()).isEqualTo("manager@kfood.local");
    assertThat(response.roles()).containsExactly("MANAGER");
    assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void shouldRejectInvalidRole() {
    var command =
        new CreateMerchantUserCommand("owner@kfood.local", "Senha@123", Set.of(UserRoleName.OWNER));

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidMerchantUserRoleException.class)
        .hasMessageContaining("OWNER");
  }

  @Test
  void shouldBlockCrossTenantAttempt() {
    var command =
        new CreateMerchantUserCommand(
            "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER));

    when(merchantTenantAccessPort.getRequiredStoreId()).thenThrow(new TenantAccessDeniedException());

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(TenantAccessDeniedException.class)
        .hasMessageContaining("another tenant");
  }

  @Test
  void shouldReturnSanitizedResponseWithoutSensitiveFields() {
    var storeId = UUID.randomUUID();
    var command =
        new CreateMerchantUserCommand(
            "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT));
    var created =
        new MerchantUserOutput(
            UUID.randomUUID(),
            "attendant@kfood.local",
            List.of("ATTENDANT"),
            UserStatus.ACTIVE,
            Instant.parse("2026-03-26T12:00:00Z"));

    when(merchantTenantAccessPort.getRequiredStoreId()).thenReturn(storeId);
    when(merchantUserManagementPort.create(
            storeId, "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT)))
        .thenReturn(created);

    var response = useCase.execute(command);

    assertThat(response.email()).isEqualTo("attendant@kfood.local");
    assertThat(response.roles()).containsExactly("ATTENDANT");
    assertThat(java.util.Arrays.stream(response.getClass().getRecordComponents())
            .map(RecordComponent::getName))
        .doesNotContain("password", "passwordHash", "storeId");
  }

}
