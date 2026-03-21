package com.kfood.catalog.api;

import java.util.List;
import java.util.UUID;

public record CatalogProductAvailabilityResponse(
    UUID productId, List<CatalogProductAvailabilityWindowResponse> windows) {}
