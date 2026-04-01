package com.kfood.merchant.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.merchant.app.TenantAccessDeniedException;
import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
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
  void shouldAllowOwnerToCreateAttendant() {
    var storeId = UUID.randomUUID();
    var command =
        new CreateMerchantUserCommand(
            "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT));

    stubSuccessfulCreation(
        storeId, Set.of(UserRoleName.OWNER), Set.of(UserRoleName.ATTENDANT), created("ATTENDANT"));

    var response = useCase.execute(command);

    assertThat(response.email()).isEqualTo("attendant@kfood.local");
    assertThat(response.roles()).containsExactly("ATTENDANT");
    assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void shouldAllowOwnerToCreateManager() {
    var storeId = UUID.randomUUID();
    var command =
        new CreateMerchantUserCommand(
            "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER));

    stubSuccessfulCreation(
        storeId, Set.of(UserRoleName.OWNER), Set.of(UserRoleName.MANAGER), created("MANAGER"));

    var response = useCase.execute(command);

    assertThat(response.email()).isEqualTo("manager@kfood.local");
    assertThat(response.roles()).containsExactly("MANAGER");
  }

  @Test
  void shouldAllowOwnerToCreateOwner() {
    var storeId = UUID.randomUUID();
    var command =
        new CreateMerchantUserCommand("owner@kfood.local", "Senha@123", Set.of(UserRoleName.OWNER));

    stubSuccessfulCreation(
        storeId, Set.of(UserRoleName.OWNER), Set.of(UserRoleName.OWNER), created("OWNER"));

    var response = useCase.execute(command);

    assertThat(response.email()).isEqualTo("owner@kfood.local");
    assertThat(response.roles()).containsExactly("OWNER");
  }

  @Test
  void shouldAllowManagerToCreateAttendant() {
    var storeId = UUID.randomUUID();
    var command =
        new CreateMerchantUserCommand(
            "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT));

    stubSuccessfulCreation(
        storeId,
        Set.of(UserRoleName.MANAGER),
        Set.of(UserRoleName.ATTENDANT),
        created("ATTENDANT"));

    var response = useCase.execute(command);

    assertThat(response.email()).isEqualTo("attendant@kfood.local");
    assertThat(response.roles()).containsExactly("ATTENDANT");
  }

  @Test
  void shouldRejectManagerCreatingManager() {
    var command =
        new CreateMerchantUserCommand(
            "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER));

    when(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles())
        .thenReturn(Set.of(UserRoleName.MANAGER));

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidMerchantUserRoleException.class)
        .hasMessageContaining("MANAGER");

    verifyNoInteractions(merchantUserManagementPort);
  }

  @Test
  void shouldRejectManagerCreatingOwner() {
    var command =
        new CreateMerchantUserCommand("owner@kfood.local", "Senha@123", Set.of(UserRoleName.OWNER));

    when(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles())
        .thenReturn(Set.of(UserRoleName.MANAGER));

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidMerchantUserRoleException.class)
        .hasMessageContaining("OWNER");

    verifyNoInteractions(merchantUserManagementPort);
  }

  @Test
  void shouldRejectAttendantCreatingMerchantUser() {
    var command =
        new CreateMerchantUserCommand(
            "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT));

    when(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles())
        .thenReturn(Set.of(UserRoleName.ATTENDANT));

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidMerchantUserRoleException.class)
        .hasMessageContaining("ATTENDANT");

    verifyNoInteractions(merchantUserManagementPort);
  }

  @Test
  void shouldRejectInvalidRequestedRoleCombination() {
    var command =
        new CreateMerchantUserCommand(
            "owner@kfood.local", "Senha@123", Set.of(UserRoleName.OWNER, UserRoleName.MANAGER));

    when(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles())
        .thenReturn(Set.of(UserRoleName.OWNER));

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidMerchantUserRoleException.class)
        .hasMessageContaining("Role combination")
        .hasMessageContaining("OWNER")
        .hasMessageContaining("MANAGER");

    verifyNoInteractions(merchantUserManagementPort);
  }

  @Test
  void shouldRejectNullRequestedRoles() {
    var command = new CreateMerchantUserCommand("owner@kfood.local", "Senha@123", null);

    when(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles())
        .thenReturn(Set.of(UserRoleName.OWNER));

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidMerchantUserRoleException.class)
        .hasMessageContaining("Role combination")
        .hasMessageContaining("[]");

    verifyNoInteractions(merchantUserManagementPort);
  }

  @Test
  void shouldRejectNullActorRoles() {
    var command =
        new CreateMerchantUserCommand(
            "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT));

    when(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles()).thenReturn(null);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidMerchantUserRoleException.class)
        .hasMessageContaining("ATTENDANT");

    verifyNoInteractions(merchantUserManagementPort);
  }

  @Test
  void shouldBlockCrossTenantAttempt() {
    var command =
        new CreateMerchantUserCommand(
            "manager@kfood.local", "Senha@123", Set.of(UserRoleName.MANAGER));

    when(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles())
        .thenThrow(new TenantAccessDeniedException());

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(TenantAccessDeniedException.class)
        .hasMessageContaining("another tenant");

    verifyNoInteractions(merchantUserManagementPort);
  }

  @Test
  void shouldReturnSanitizedResponseWithoutSensitiveFields() {
    var storeId = UUID.randomUUID();
    var command =
        new CreateMerchantUserCommand(
            "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT));

    when(merchantTenantAccessPort.getRequiredStoreId()).thenReturn(storeId);
    when(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles())
        .thenReturn(Set.of(UserRoleName.MANAGER));
    when(merchantUserManagementPort.create(
            storeId, "attendant@kfood.local", "Senha@123", Set.of(UserRoleName.ATTENDANT)))
        .thenReturn(created("ATTENDANT"));

    var response = useCase.execute(command);

    assertThat(response.email()).isEqualTo("attendant@kfood.local");
    assertThat(response.roles()).containsExactly("ATTENDANT");
    assertThat(
            java.util.Arrays.stream(response.getClass().getRecordComponents())
                .map(RecordComponent::getName))
        .doesNotContain("password", "passwordHash", "storeId");
  }

  @Test
  void shouldBuildInvalidRoleExceptionForNullRoleCombination() {
    var exception = new InvalidMerchantUserRoleException((Set<UserRoleName>) null);

    assertThat(exception.getMessage())
        .isEqualTo("Role combination not allowed for merchant user creation: []");
  }

  private void stubSuccessfulCreation(
      UUID storeId,
      Set<UserRoleName> actorRoles,
      Set<UserRoleName> requestedRoles,
      MerchantUserOutput created) {
    when(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles()).thenReturn(actorRoles);
    when(merchantTenantAccessPort.getRequiredStoreId()).thenReturn(storeId);
    when(merchantUserManagementPort.create(storeId, created.email(), "Senha@123", requestedRoles))
        .thenReturn(created);
  }

  private MerchantUserOutput created(String role) {
    return new MerchantUserOutput(
        UUID.randomUUID(),
        role.toLowerCase() + "@kfood.local",
        List.of(role),
        UserStatus.ACTIVE,
        Instant.parse("2026-03-26T12:00:00Z"));
  }
}
