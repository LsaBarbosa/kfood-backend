package com.kfood.merchant.application.user.port;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.merchant.application.user.MerchantUserOutput;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MerchantUserManagementPort {

  MerchantUserOutput create(
      UUID storeId, String email, String rawPassword, Set<UserRoleName> roles);

  List<MerchantUserOutput> listByStoreId(UUID storeId);
}
