package com.kfood.merchant.api;

import com.kfood.merchant.domain.StoreStatus;
import java.util.List;

public record PublicStoreResponse(
    String slug,
    String name,
    StoreStatus status,
    String phone,
    List<PublicStoreHourResponse> hours,
    List<PublicDeliveryZoneResponse> deliveryZones) {}
