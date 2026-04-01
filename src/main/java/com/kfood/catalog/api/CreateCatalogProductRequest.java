package com.kfood.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateCatalogProductRequest(
    @NotNull UUID categoryId,
    @NotBlank @Size(max = 160) String name,
    @NotBlank @Size(max = 500) String description,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal basePrice,
    @Size(max = 500) String imageUrl,
    @NotNull @PositiveOrZero Integer sortOrder,
    @NotNull Boolean active,
    @NotNull Boolean paused) {}
