package com.kfood.merchant.api;

import com.kfood.merchant.domain.StoreStatus;
import java.time.Instant;
import java.util.UUID;

public record CreateStoreResponse(UUID id, String slug, StoreStatus status, Instant createdAt) {}
