package com.kfood.merchant.application.user;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.http.HttpStatus;

public class InvalidMerchantUserRoleException extends BusinessException {

  public InvalidMerchantUserRoleException(UserRoleName roleName) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Role not allowed for merchant user creation: " + roleName,
        HttpStatus.BAD_REQUEST);
  }

  public InvalidMerchantUserRoleException(Set<UserRoleName> roleNames) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Role combination not allowed for merchant user creation: " + normalize(roleNames),
        HttpStatus.BAD_REQUEST);
  }

  private static Set<UserRoleName> normalize(Set<UserRoleName> roleNames) {
    var normalizedRoleNames = EnumSet.noneOf(UserRoleName.class);
    if (roleNames != null) {
      normalizedRoleNames.addAll(roleNames);
    }
    return normalizedRoleNames;
  }
}
