package com.kfood.merchant.application.user;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

public class CreateMerchantUserUseCase {

  private static final Set<UserRoleName> ALLOWED_ROLES =
      EnumSet.of(UserRoleName.MANAGER, UserRoleName.ATTENDANT);

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
    validateRoles(command.roles());

    var storeId = merchantTenantAccessPort.getRequiredStoreId();
    return merchantUserManagementPort.create(
        storeId, command.email().trim(), command.password(), command.roles());
  }

  private void validateRoles(Set<UserRoleName> roles) {
    for (var role : roles) {
      if (!ALLOWED_ROLES.contains(role)) {
        throw new InvalidMerchantUserRoleException(role);
      }
    }
  }
}
