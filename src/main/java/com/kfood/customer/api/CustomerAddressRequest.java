package com.kfood.customer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerAddressRequest(
    @NotBlank @Size(max = 60) String label,
    @NotBlank @Pattern(regexp = "^\\d{5}-?\\d{3}$") String zipCode,
    @NotBlank @Size(max = 160) String street,
    @NotBlank @Size(max = 20) String number,
    @NotBlank @Size(max = 100) String district,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String state,
    @Size(max = 120) String complement,
    Boolean mainAddress) {}
