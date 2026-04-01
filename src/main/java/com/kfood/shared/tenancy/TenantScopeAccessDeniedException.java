package com.kfood.shared.tenancy;

import org.springframework.security.access.AccessDeniedException;

public class TenantScopeAccessDeniedException extends AccessDeniedException {

  public TenantScopeAccessDeniedException(String message) {
    super(message);
  }
}
