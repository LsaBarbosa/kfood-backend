package com.kfood.shared.tenancy;

import com.kfood.shared.security.TenantAwarePrincipal;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCurrentTenantProvider implements CurrentTenantProvider {

  @Override
  public UUID getRequiredStoreId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Unauthenticated request");
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof TenantAwarePrincipal tenantAwarePrincipal) {
      UUID storeId = tenantAwarePrincipal.getStoreId();
      if (storeId != null) {
        return storeId;
      }
    }

    throw new AccessDeniedException("Authenticated user is not bound to a store");
  }
}
