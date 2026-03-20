package com.kfood.shared.security;

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

class SpringSecurityCurrentAuthenticatedUserProviderTest {

  private final SpringSecurityCurrentAuthenticatedUserProvider provider =
      new SpringSecurityCurrentAuthenticatedUserProvider();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnAuthenticatedUserId() {
    var userId = UUID.randomUUID();
    var principal =
        new AuthenticatedUser(userId, "owner@kfood.local", UUID.randomUUID(), List.of("OWNER"));
    var authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThat(provider.getRequiredUserId()).isEqualTo(userId);
  }

  @Test
  void shouldRejectWhenUnauthenticated() {
    assertThatThrownBy(provider::getRequiredUserId)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Unauthenticated request");
  }

  @Test
  void shouldRejectWhenAuthenticationIsPresentButNotAuthenticated() {
    var authentication =
        new UsernamePasswordAuthenticationToken("plain-user", null, List.of()) {
          @Override
          public boolean isAuthenticated() {
            return false;
          }
        };
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThatThrownBy(provider::getRequiredUserId)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Unauthenticated request");
  }

  @Test
  void shouldRejectWhenPrincipalDoesNotExposeIdentifier() {
    var authentication = new UsernamePasswordAuthenticationToken("plain-user", null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThatThrownBy(provider::getRequiredUserId)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("does not expose an identifier");
  }

  @Test
  void shouldRejectWhenPrincipalExposesNullIdentifier() {
    var principal =
        new AuthenticatedUser(null, "owner@kfood.local", UUID.randomUUID(), List.of("OWNER"));
    var authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThatThrownBy(provider::getRequiredUserId)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("does not expose an identifier");
  }
}
