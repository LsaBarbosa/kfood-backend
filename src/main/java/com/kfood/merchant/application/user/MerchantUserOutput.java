package com.kfood.merchant.application.user;

import com.kfood.identity.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MerchantUserOutput(
    UUID id, String email, List<String> roles, UserStatus status, Instant createdAt) {}
