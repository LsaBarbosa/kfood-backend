package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogProductAvailabilityResponse;
import com.kfood.catalog.api.CatalogProductAvailabilityWindowResponse;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductAvailabilityWindow;

public final class CatalogProductAvailabilityMapper {

  private CatalogProductAvailabilityMapper() {}

  public static CatalogProductAvailabilityResponse toResponse(CatalogProduct product) {
    return new CatalogProductAvailabilityResponse(
        product.getId(),
        product.getAvailabilityWindows().stream()
            .map(CatalogProductAvailabilityMapper::toWindowResponse)
            .toList());
  }

  private static CatalogProductAvailabilityWindowResponse toWindowResponse(
      CatalogProductAvailabilityWindow window) {
    return new CatalogProductAvailabilityWindowResponse(
        window.getId(),
        window.getDayOfWeek(),
        window.getStartTime(),
        window.getEndTime(),
        window.isActive());
  }
}
