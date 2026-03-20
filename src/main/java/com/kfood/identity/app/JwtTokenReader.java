package com.kfood.identity.app;

import java.util.List;
import java.util.UUID;

public interface JwtTokenReader {

  AuthenticatedPrincipal read(String token);

  record AuthenticatedPrincipal(UUID userId, String email, UUID tenantId, List<String> roles) {}
}
