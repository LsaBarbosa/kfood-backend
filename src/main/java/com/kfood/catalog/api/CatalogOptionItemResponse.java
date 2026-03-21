package com.kfood.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogOptionItemResponse(
    UUID id, String name, BigDecimal extraPrice, boolean active, int sortOrder) {}
