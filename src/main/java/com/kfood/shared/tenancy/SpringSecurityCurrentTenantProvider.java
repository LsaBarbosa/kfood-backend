package com.kfood.shared.tenancy;

import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.shared.security.AuthenticatedPrincipal;
import com.kfood.shared.security.TenantAwarePrincipal;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCurrentTenantProvider implements CurrentTenantProvider {

  private final IdentityUserRepository identityUserRepository;

  public SpringSecurityCurrentTenantProvider(
      ObjectProvider<IdentityUserRepository> identityUserRepositoryProvider) {
    identityUserRepository = identityUserRepositoryProvider.getIfAvailable();
  }

  @Override
  public UUID getRequiredStoreId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Unauthenticated request");
    }

    var principal = authentication.getPrincipal();

    if (!(principal instanceof TenantAwarePrincipal tenantAwarePrincipal)) {
      throw new TenantScopeAccessDeniedException("Authenticated user is not bound to a store");
    }

    var principalStoreId = tenantAwarePrincipal.getStoreId();

    if (!(principal instanceof AuthenticatedPrincipal authenticatedPrincipal)
        || identityUserRepository == null) {
      if (principalStoreId != null) {
        return principalStoreId;
      }
      throw new TenantScopeAccessDeniedException("Authenticated user is not bound to a store");
    }

    var persistedUser =
        identityUserRepository
            .findById(authenticatedPrincipal.getUserId())
            .orElseThrow(
                () ->
                    new TenantScopeAccessDeniedException("Authenticated user cannot be resolved"));

    var persistedStoreId = persistedUser.getStoreId();

    if (principalStoreId != null
        && persistedStoreId != null
        && !principalStoreId.equals(persistedStoreId)) {
      throw new TenantScopeAccessDeniedException("Authenticated user cannot access another tenant");
    }

    var resolvedStoreId = principalStoreId != null ? principalStoreId : persistedStoreId;
    if (resolvedStoreId != null) {
      return resolvedStoreId;
    }

    throw new TenantScopeAccessDeniedException("Authenticated user is not bound to a store");
  }
}
