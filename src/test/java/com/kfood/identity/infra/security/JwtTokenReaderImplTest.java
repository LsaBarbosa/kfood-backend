package com.kfood.identity.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.shared.config.AppProperties;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenReaderImplTest {

  private final AppProperties appProperties = appProperties();
  private final JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(appProperties);
  private final JwtTokenReaderImpl jwtTokenReader = new JwtTokenReaderImpl(appProperties);

  @Test
  @DisplayName("should read valid token")
  void shouldReadValidToken() {
    var userId = UUID.randomUUID();
    var tenantId = UUID.randomUUID();

    var user =
        new IdentityUserEntity(
            userId, tenantId, "owner@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));

    var token = jwtTokenService.generateToken(user);
    var principal = jwtTokenReader.read(token);

    assertThat(principal.userId()).isEqualTo(userId);
    assertThat(principal.email()).isEqualTo("owner@kfood.local");
    assertThat(principal.tenantId()).isEqualTo(tenantId);
    assertThat(principal.roles()).containsExactly("OWNER");
  }

  @Test
  @DisplayName("should reject malformed token")
  void shouldRejectMalformedToken() {
    assertThatThrownBy(() -> jwtTokenReader.read("token-invalido")).isInstanceOf(Exception.class);
  }

  private AppProperties appProperties() {
    var properties = new AppProperties();
    properties.getSecurity().setJwtSecret("12345678901234567890123456789012");
    properties.getSecurity().setJwtExpirationSeconds(3600);
    return properties;
  }
}
