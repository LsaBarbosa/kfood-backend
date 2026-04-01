package com.kfood.catalog.api;

import java.util.UUID;

public record CatalogProductPauseResponse(UUID id, boolean paused, boolean active) {}
