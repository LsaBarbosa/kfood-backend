package com.kfood.merchant.api;

import com.kfood.identity.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MerchantUserResponse(
    UUID id, String email, List<String> roles, UserStatus status, Instant createdAt) {}
