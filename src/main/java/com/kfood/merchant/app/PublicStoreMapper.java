package com.kfood.merchant.app;

import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import java.util.List;

public final class PublicStoreMapper {

  private PublicStoreMapper() {}

  public static PublicStoreOutput toOutput(
      Store store,
      List<PublicStoreHourOutput> hours,
      List<PublicDeliveryZoneOutput> deliveryZones) {
    return new PublicStoreOutput(
        store.getSlug(),
        store.getName(),
        store.getStatus(),
        store.getPhone(),
        hours,
        deliveryZones);
  }

  public static PublicStoreHourOutput toHourOutput(StoreBusinessHour hour) {
    return new PublicStoreHourOutput(
        hour.getDayOfWeek(), hour.getOpenTime(), hour.getCloseTime(), hour.isClosed());
  }

  public static PublicDeliveryZoneOutput toDeliveryZoneOutput(DeliveryZone zone) {
    return new PublicDeliveryZoneOutput(
        zone.getZoneName(), zone.getFeeAmount(), zone.getMinOrderAmount());
  }

  public static PublicStoreMenuProductOutput toMenuProductOutput(CatalogProduct product) {
    return toMenuProductOutput(product, product.getOptionGroups());
  }

  public static PublicStoreMenuProductOutput toMenuProductOutput(
      CatalogProduct product, List<CatalogOptionGroup> optionGroups) {
    return new PublicStoreMenuProductOutput(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getBasePrice(),
        product.getImageUrl(),
        product.isPaused(),
        optionGroups.stream()
            .filter(CatalogOptionGroup::isActive)
            .map(PublicStoreMapper::toMenuOptionGroupResponse)
            .toList());
  }

  static PublicStoreMenuOptionGroupOutput toMenuOptionGroupResponse(CatalogOptionGroup group) {
    return new PublicStoreMenuOptionGroupOutput(
        group.getId(),
        group.getName(),
        group.getMinSelect(),
        group.getMaxSelect(),
        group.isRequired(),
        group.getItems().stream()
            .filter(CatalogOptionItem::isActive)
            .map(PublicStoreMapper::toMenuOptionItemResponse)
            .toList());
  }

  static PublicStoreMenuOptionItemOutput toMenuOptionItemResponse(CatalogOptionItem item) {
    return new PublicStoreMenuOptionItemOutput(
        item.getId(), item.getName(), item.getExtraPrice(), item.getSortOrder());
  }
}
