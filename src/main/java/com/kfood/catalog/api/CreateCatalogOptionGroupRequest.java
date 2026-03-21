package com.kfood.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateCatalogOptionGroupRequest(
    @NotBlank @Size(max = 120) String name,
    @PositiveOrZero Integer minSelect,
    @PositiveOrZero Integer maxSelect,
    Boolean required,
    Boolean active,
    @Valid List<CreateCatalogOptionItemRequest> items) {}
