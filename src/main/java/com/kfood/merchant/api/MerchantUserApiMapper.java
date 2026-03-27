package com.kfood.merchant.api;

import com.kfood.merchant.application.user.MerchantUserOutput;

public final class MerchantUserApiMapper {

  private MerchantUserApiMapper() {}

  public static MerchantUserResponse toResponse(MerchantUserOutput output) {
    return new MerchantUserResponse(
        output.id(), output.email(), output.roles(), output.status(), output.createdAt());
  }
}
