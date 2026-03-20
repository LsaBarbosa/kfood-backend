package com.kfood.identity.infra.security;

import com.kfood.identity.app.JwtTokenReader;
import com.kfood.shared.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenReaderImpl implements JwtTokenReader {

  private final SecretKey secretKey;

  public JwtTokenReaderImpl(AppProperties appProperties) {
    secretKey =
        Keys.hmacShaKeyFor(
            appProperties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  @SuppressWarnings("unchecked")
  public AuthenticatedPrincipal read(String token) {
    var jws = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
    Claims claims = jws.getPayload();

    var userId = claims.get("user_id", String.class);
    var tenantId = claims.get("tenant_id", String.class);
    var roles = claims.get("roles", List.class);
    var email = claims.getSubject();

    return new AuthenticatedPrincipal(
        UUID.fromString(userId),
        email,
        tenantId == null ? null : UUID.fromString(tenantId),
        roles == null ? List.of() : (List<String>) roles);
  }
}
