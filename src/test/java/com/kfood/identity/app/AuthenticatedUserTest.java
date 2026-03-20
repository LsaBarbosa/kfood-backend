package com.kfood.identity.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

  @Test
  void shouldExposeUserDetailsContract() {
    var user =
        new AuthenticatedUser(
            UUID.randomUUID(), "owner@kfood.local", UUID.randomUUID(), List.of("OWNER", "MANAGER"));

    assertThat(user.getUserId()).isNotNull();
    assertThat(user.getTenantId()).isNotNull();
    assertThat(user.getUsername()).isEqualTo("owner@kfood.local");
    assertThat(user.getPassword()).isNull();
    assertThat(user.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_OWNER", "ROLE_MANAGER");
    assertThat(user.isAccountNonExpired()).isTrue();
    assertThat(user.isAccountNonLocked()).isTrue();
    assertThat(user.isCredentialsNonExpired()).isTrue();
    assertThat(user.isEnabled()).isTrue();
  }
}
