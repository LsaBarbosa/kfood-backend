package com.kfood.customer.api;

import java.util.UUID;

public record CustomerResponse(
    UUID id, String name, String phone, String email, UUID mainAddressId) {}
