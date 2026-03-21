package com.kfood.catalog.api;

import java.util.UUID;

public record CatalogCategoryResponse(UUID id, String name, int sortOrder, boolean active) {}
