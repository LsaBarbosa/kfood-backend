package com.kfood.merchant.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AuthenticatedUserNotFoundException extends BusinessException {

  public AuthenticatedUserNotFoundException(UUID userId) {
    super(
        ErrorCode.AUTH_INVALID_CREDENTIALS,
        "Authenticated user not found for id: " + userId,
        HttpStatus.UNAUTHORIZED);
  }
}
