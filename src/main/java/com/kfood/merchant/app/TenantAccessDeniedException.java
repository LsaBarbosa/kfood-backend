package com.kfood.merchant.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class TenantAccessDeniedException extends BusinessException {

  public TenantAccessDeniedException() {
    super(
        ErrorCode.TENANT_ACCESS_DENIED,
        "Authenticated user cannot operate on another tenant.",
        HttpStatus.FORBIDDEN);
  }
}
