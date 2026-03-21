package com.kfood.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateCatalogCategoryRequest(
    @NotBlank @Size(max = 120) String name, @NotNull @PositiveOrZero Integer sortOrder) {}
