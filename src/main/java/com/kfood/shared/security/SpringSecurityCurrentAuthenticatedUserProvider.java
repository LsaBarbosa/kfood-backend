package com.kfood.shared.security;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCurrentAuthenticatedUserProvider
    implements CurrentAuthenticatedUserProvider {

  @Override
  public UUID getRequiredUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Unauthenticated request");
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
      var userId = authenticatedPrincipal.getUserId();
      if (userId != null) {
        return userId;
      }
    }

    throw new AccessDeniedException("Authenticated user does not expose an identifier");
  }
}
