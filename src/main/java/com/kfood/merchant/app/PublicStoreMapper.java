package com.kfood.merchant.app;

import java.util.List;

public final class PublicStoreMapper {

  private PublicStoreMapper() {}

  public static PublicStoreOutput toOutput(
      MerchantViews.StoreView store,
      List<PublicStoreHourOutput> hours,
      List<PublicDeliveryZoneOutput> deliveryZones) {
    return new PublicStoreOutput(
        store.slug(), store.name(), store.status(), store.phone(), hours, deliveryZones);
  }

  public static PublicStoreHourOutput toHourOutput(MerchantViews.StoreHourView hour) {
    return new PublicStoreHourOutput(
        hour.dayOfWeek(), hour.openTime(), hour.closeTime(), hour.closed());
  }

  public static PublicDeliveryZoneOutput toDeliveryZoneOutput(MerchantViews.DeliveryZoneView zone) {
    return new PublicDeliveryZoneOutput(zone.zoneName(), zone.feeAmount(), zone.minOrderAmount());
  }

  public static PublicStoreMenuProductOutput toMenuProductOutput(
      MerchantViews.PublicStoreMenuProductView product) {
    return new PublicStoreMenuProductOutput(
        product.id(),
        product.name(),
        product.description(),
        product.basePrice(),
        product.imageUrl(),
        product.paused(),
        product.optionGroups().stream().map(PublicStoreMapper::toMenuOptionGroupResponse).toList());
  }

  static PublicStoreMenuOptionGroupOutput toMenuOptionGroupResponse(
      MerchantViews.PublicStoreMenuOptionGroupView group) {
    return new PublicStoreMenuOptionGroupOutput(
        group.id(),
        group.name(),
        group.minSelect(),
        group.maxSelect(),
        group.required(),
        group.items().stream().map(PublicStoreMapper::toMenuOptionItemResponse).toList());
  }

  static PublicStoreMenuOptionItemOutput toMenuOptionItemResponse(
      MerchantViews.PublicStoreMenuOptionItemView item) {
    return new PublicStoreMenuOptionItemOutput(
        item.id(), item.name(), item.extraPrice(), item.sortOrder());
  }
}
