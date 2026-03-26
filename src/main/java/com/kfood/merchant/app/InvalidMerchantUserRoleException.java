package com.kfood.merchant.app;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidMerchantUserRoleException extends BusinessException {

  public InvalidMerchantUserRoleException(UserRoleName roleName) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Role not allowed for merchant user creation: " + roleName,
        HttpStatus.BAD_REQUEST);
  }
}
