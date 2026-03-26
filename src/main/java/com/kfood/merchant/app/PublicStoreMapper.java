package com.kfood.merchant.app;

import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.merchant.api.PublicDeliveryZoneResponse;
import com.kfood.merchant.api.PublicStoreHourResponse;
import com.kfood.merchant.api.PublicStoreMenuOptionGroupResponse;
import com.kfood.merchant.api.PublicStoreMenuOptionItemResponse;
import com.kfood.merchant.api.PublicStoreMenuProductResponse;
import com.kfood.merchant.api.PublicStoreResponse;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import java.util.List;

public final class PublicStoreMapper {

  private PublicStoreMapper() {}

  public static PublicStoreResponse toResponse(
      Store store,
      List<PublicStoreHourResponse> hours,
      List<PublicDeliveryZoneResponse> deliveryZones) {
    return new PublicStoreResponse(
        store.getSlug(),
        store.getName(),
        store.getStatus(),
        store.getPhone(),
        hours,
        deliveryZones);
  }

  public static PublicStoreHourResponse toHourResponse(StoreBusinessHour hour) {
    return new PublicStoreHourResponse(
        hour.getDayOfWeek(), hour.getOpenTime(), hour.getCloseTime(), hour.isClosed());
  }

  public static PublicDeliveryZoneResponse toDeliveryZoneResponse(DeliveryZone zone) {
    return new PublicDeliveryZoneResponse(
        zone.getZoneName(), zone.getFeeAmount(), zone.getMinOrderAmount());
  }

  public static PublicStoreMenuProductResponse toMenuProductResponse(CatalogProduct product) {
    return new PublicStoreMenuProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getBasePrice(),
        product.getImageUrl(),
        product.isPaused(),
        product.getOptionGroups().stream()
            .filter(CatalogOptionGroup::isActive)
            .map(PublicStoreMapper::toMenuOptionGroupResponse)
            .toList());
  }

  static PublicStoreMenuOptionGroupResponse toMenuOptionGroupResponse(CatalogOptionGroup group) {
    return new PublicStoreMenuOptionGroupResponse(
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

  static PublicStoreMenuOptionItemResponse toMenuOptionItemResponse(CatalogOptionItem item) {
    return new PublicStoreMenuOptionItemResponse(
        item.getId(), item.getName(), item.getExtraPrice(), item.getSortOrder());
  }
}
