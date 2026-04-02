package com.kfood.merchant.app;

import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import java.util.UUID;

public record StoreDetailsOutput(
    UUID id,
    String slug,
    String name,
    StoreStatus status,
    String phone,
    String timezone,
    StoreCategory category,
    StoreAddressOutput address,
    boolean hoursConfigured,
    boolean deliveryZonesConfigured) {

  public StoreDetailsOutput(
      UUID id,
      String slug,
      String name,
      StoreStatus status,
      String phone,
      String timezone,
      boolean hoursConfigured,
      boolean deliveryZonesConfigured) {
    this(
        id,
        slug,
        name,
        status,
        phone,
        timezone,
        null,
        null,
        hoursConfigured,
        deliveryZonesConfigured);
  }
}
