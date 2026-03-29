package com.kfood.merchant.app;

import com.kfood.merchant.domain.StoreStatus;
import java.time.Instant;
import java.util.UUID;

public record CreateStoreOutput(UUID id, String slug, StoreStatus status, Instant createdAt) {}
