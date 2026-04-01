package com.kfood.identity.api;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
    String accessToken, String tokenType, long expiresIn, AuthenticatedUserResponse user) {

  public record AuthenticatedUserResponse(
      UUID id, String email, Set<String> roles, UUID tenantId, String status) {}
}
