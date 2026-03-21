package com.kfood.catalog.api;

import java.util.UUID;

public record CatalogOptionGroupResponse(
    UUID id,
    UUID productId,
    String name,
    int minSelect,
    int maxSelect,
    boolean required,
    boolean active) {}
