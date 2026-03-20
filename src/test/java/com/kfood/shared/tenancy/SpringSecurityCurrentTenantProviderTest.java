package com.kfood.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.identity.app.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SpringSecurityCurrentTenantProviderTest {

  private final SpringSecurityCurrentTenantProvider provider =
      new SpringSecurityCurrentTenantProvider();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnStoreIdFromTenantAwarePrincipal() {
    var storeId = UUID.randomUUID();
    var principal =
        new AuthenticatedUser(UUID.randomUUID(), "owner@kfood.local", storeId, List.of("OWNER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    assertThat(provider.getRequiredStoreId()).isEqualTo(storeId);
  }

  @Test
  void shouldRejectWhenAuthenticationIsMissing() {
    assertThatThrownBy(provider::getRequiredStoreId)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Unauthenticated request");
  }

  @Test
  void shouldRejectWhenPrincipalIsNotTenantAware() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("plain-user", null, List.of()));

    assertThatThrownBy(provider::getRequiredStoreId)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Authenticated user is not bound to a store");
  }

  @Test
  void shouldRejectWhenTenantAwarePrincipalHasNoStore() {
    var principal =
        new AuthenticatedUser(UUID.randomUUID(), "admin@kfood.local", null, List.of("ADMIN"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    assertThatThrownBy(provider::getRequiredStoreId)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Authenticated user is not bound to a store");
  }
}
