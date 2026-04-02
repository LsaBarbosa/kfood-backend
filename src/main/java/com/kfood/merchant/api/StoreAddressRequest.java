package com.kfood.merchant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StoreAddressRequest(
    @NotBlank @Pattern(
            regexp = "^\\d{5}-?\\d{3}$",
            message = "zipCode must contain 8 digits, optionally with hyphen")
        String zipCode,
    @NotBlank @Size(max = 160) String street,
    @NotBlank @Size(max = 20) String number,
    @NotBlank @Size(max = 100) String district,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "state must have 2 letters") String state) {}
