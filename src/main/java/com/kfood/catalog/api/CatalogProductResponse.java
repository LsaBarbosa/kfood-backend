package com.kfood.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogProductResponse(
    UUID id,
    UUID categoryId,
    String name,
    String description,
    BigDecimal basePrice,
    String imageUrl,
    int sortOrder,
    boolean active,
    boolean paused) {}
