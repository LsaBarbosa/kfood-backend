package com.kfood.customer.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertCustomerRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 20) String phone,
    @Email @Size(max = 160) String email) {}
