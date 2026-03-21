package com.kfood.catalog.api;

import java.util.List;
import java.util.UUID;

public record CatalogOptionGroupResponse(
    UUID id,
    UUID productId,
    String name,
    int minSelect,
    int maxSelect,
    boolean required,
    boolean active,
    List<CatalogOptionItemResponse> items) {}
