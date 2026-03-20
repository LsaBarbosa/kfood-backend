package com.kfood.identity.infra.security;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.shared.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

  private final AppProperties appProperties;
  private final SecretKey secretKey;

  public JwtTokenServiceImpl(AppProperties appProperties) {
    this.appProperties = appProperties;
    secretKey =
        Keys.hmacShaKeyFor(
            appProperties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String generateToken(IdentityUserEntity user) {
    var now = Instant.now();
    var expiresAt = now.plusSeconds(appProperties.getSecurity().getJwtExpirationSeconds());
    var roles = user.getRoles().stream().map(role -> role.getRoleName().name()).sorted().toList();

    return Jwts.builder()
        .subject(user.getEmail())
        .claim("userId", user.getId().toString())
        .claim("tenantId", uuidToString(user.getStoreId()))
        .claim("roles", roles)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();
  }

  private String uuidToString(UUID value) {
    return value == null ? null : value.toString();
  }
}
