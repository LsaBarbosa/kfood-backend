package com.kfood.catalog.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCatalogProductPauseRequest(
    @NotNull Boolean paused, @Size(max = 255) String reason) {}
