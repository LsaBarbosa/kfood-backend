package com.kfood.merchant.application.user.port;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.merchant.application.user.MerchantUserOutput;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MerchantUserManagementPort {

  MerchantUserOutput create(
      UUID storeId, String email, String rawPassword, Set<UserRoleName> roles, UserStatus status);

  List<MerchantUserOutput> listByStoreId(UUID storeId);
}
