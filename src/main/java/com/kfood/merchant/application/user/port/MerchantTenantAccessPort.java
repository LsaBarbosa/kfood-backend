package com.kfood.merchant.application.user.port;

import com.kfood.identity.domain.UserRoleName;
import java.util.Set;
import java.util.UUID;

public interface MerchantTenantAccessPort {

  UUID getRequiredStoreId();

  Set<UserRoleName> getRequiredAuthenticatedUserRoles();
}
