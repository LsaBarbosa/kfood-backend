package com.kfood.merchant.app;

import com.kfood.merchant.domain.StoreStatus;
import java.util.List;

public record PublicStoreOutput(
    String slug,
    String name,
    StoreStatus status,
    String phone,
    List<PublicStoreHourOutput> hours,
    List<PublicDeliveryZoneOutput> deliveryZones) {}
