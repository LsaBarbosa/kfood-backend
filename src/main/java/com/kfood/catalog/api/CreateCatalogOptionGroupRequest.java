package com.kfood.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateCatalogOptionGroupRequest(
    @NotBlank @Size(max = 120) String name,
    @PositiveOrZero Integer minSelect,
    @PositiveOrZero Integer maxSelect,
    Boolean required,
    Boolean active) {}
