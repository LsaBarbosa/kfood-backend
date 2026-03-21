package com.kfood.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateCatalogProductAvailabilityRequest(
    @NotNull List<@Valid CatalogProductAvailabilityWindowRequest> windows) {}
