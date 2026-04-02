package com.kfood.merchant.application.user;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

public class CreateMerchantUserUseCase {

  private static final Set<UserRoleName> OWNER_ALLOWED_ROLES =
      EnumSet.of(UserRoleName.OWNER, UserRoleName.MANAGER, UserRoleName.ATTENDANT);
  private static final Set<UserRoleName> MANAGER_ALLOWED_ROLES = EnumSet.of(UserRoleName.ATTENDANT);

  private final MerchantUserManagementPort merchantUserManagementPort;
  private final MerchantTenantAccessPort merchantTenantAccessPort;

  public CreateMerchantUserUseCase(
      MerchantUserManagementPort merchantUserManagementPort,
      MerchantTenantAccessPort merchantTenantAccessPort) {
    this.merchantUserManagementPort = merchantUserManagementPort;
    this.merchantTenantAccessPort = merchantTenantAccessPort;
  }

  @Transactional
  public MerchantUserOutput execute(CreateMerchantUserCommand command) {
    var actorRoles = normalizeRoles(merchantTenantAccessPort.getRequiredAuthenticatedUserRoles());
    var requestedRoles = normalizeRoles(command.roles());

    validateRoles(actorRoles, requestedRoles);

    var storeId = merchantTenantAccessPort.getRequiredStoreId();
    return merchantUserManagementPort.create(
        storeId,
        command.email().trim(),
        command.temporaryPassword(),
        requestedRoles,
        command.status());
  }

  private void validateRoles(Set<UserRoleName> actorRoles, Set<UserRoleName> requestedRoles) {
    if (requestedRoles.size() != 1) {
      throw new InvalidMerchantUserRoleException(requestedRoles);
    }

    var requestedRole = requestedRoles.iterator().next();
    if (!allowedRolesFor(actorRoles).contains(requestedRole)) {
      throw new InvalidMerchantUserRoleException(requestedRole);
    }
  }

  private Set<UserRoleName> allowedRolesFor(Set<UserRoleName> actorRoles) {
    if (actorRoles.contains(UserRoleName.OWNER)) {
      return OWNER_ALLOWED_ROLES;
    }
    if (actorRoles.contains(UserRoleName.MANAGER)) {
      return MANAGER_ALLOWED_ROLES;
    }

    return EnumSet.noneOf(UserRoleName.class);
  }

  private EnumSet<UserRoleName> normalizeRoles(Set<UserRoleName> roles) {
    var normalizedRoles = EnumSet.noneOf(UserRoleName.class);
    if (roles != null) {
      normalizedRoles.addAll(roles);
    }
    return normalizedRoles;
  }
}
