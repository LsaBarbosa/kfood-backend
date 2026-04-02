package com.kfood.merchant.api;

import com.kfood.merchant.domain.StoreCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

public record CreateStoreRequest(
    @NotBlank @Size(max = 160) String name,
    @NotBlank @Size(max = 120) @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "slug must contain only lowercase letters, numbers and hyphens")
        String slug,
    @NotBlank @CNPJ String cnpj,
    @NotBlank @Pattern(regexp = "^\\d{10,15}$", message = "phone must contain between 10 and 15 digits") String phone,
    @NotBlank @Size(max = 60) String timezone,
    @NotNull StoreCategory category,
    @Valid @NotNull StoreAddressRequest address) {}
