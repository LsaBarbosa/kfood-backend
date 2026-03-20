package com.kfood.identity.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class UserAuthenticationLockedException extends BusinessException {

  public UserAuthenticationLockedException(String message) {
    super(ErrorCode.AUTH_INVALID_CREDENTIALS, message, HttpStatus.LOCKED);
  }
}
