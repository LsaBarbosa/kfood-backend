package com.kfood.merchant.api;

import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import java.util.UUID;

public record StoreResponse(
    UUID id,
    String name,
    String slug,
    String cnpj,
    String phone,
    String timezone,
    StoreCategory category,
    StoreAddressResponse address,
    StoreStatus status) {}
