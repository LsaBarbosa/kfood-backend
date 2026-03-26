package com.kfood.merchant.app;

import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.merchant.api.MerchantUserResponse;
import java.util.Comparator;

public final class MerchantUserMapper {

  private MerchantUserMapper() {}

  public static MerchantUserResponse toResponse(IdentityUserEntity user) {
    return new MerchantUserResponse(
        user.getId(),
        user.getEmail(),
        user.getRoles().stream()
            .map(role -> role.getRoleName().name())
            .sorted(Comparator.naturalOrder())
            .toList(),
        user.getStatus(),
        user.getCreatedAt());
  }
}
