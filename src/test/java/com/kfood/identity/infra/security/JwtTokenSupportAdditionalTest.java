package com.kfood.identity.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.shared.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenSupportAdditionalTest {

  private final AppProperties appProperties = appProperties();
  private final JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(appProperties);
  private final JwtTokenReaderImpl jwtTokenReader = new JwtTokenReaderImpl(appProperties);

  @Test
  void shouldGenerateTokenWithoutTenantIdForGlobalUser() {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(), null, "admin@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.ADMIN));

    var token = jwtTokenService.generateToken(user);
    var principal = jwtTokenReader.read(token);

    assertThat(principal.tenantId()).isNull();
    assertThat(principal.roles()).containsExactly("ADMIN");
  }

  @Test
  void shouldReadTokenWithoutRolesClaim() {
    var now = Instant.now();
    var token =
        Jwts.builder()
            .subject("owner@kfood.local")
            .claim("user_id", UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(3600)))
            .signWith(
                Keys.hmacShaKeyFor(
                    appProperties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8)))
            .compact();

    var principal = jwtTokenReader.read(token);

    assertThat(principal.email()).isEqualTo("owner@kfood.local");
    assertThat(principal.tenantId()).isNull();
    assertThat(principal.roles()).isEmpty();
  }

  private AppProperties appProperties() {
    var properties = new AppProperties();
    properties.getSecurity().setJwtSecret("12345678901234567890123456789012");
    properties.getSecurity().setJwtExpirationSeconds(3600);
    return properties;
  }
}
