package com.kfood.merchant.api;

import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import java.util.UUID;

public record StoreDetailsResponse(
    UUID id,
    String slug,
    String name,
    StoreStatus status,
    String phone,
    String timezone,
    StoreCategory category,
    StoreAddressResponse address,
    boolean hoursConfigured,
    boolean deliveryZonesConfigured) {}
