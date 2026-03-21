package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogProductResponse;
import com.kfood.catalog.infra.persistence.CatalogProduct;

public final class CatalogProductMapper {

  private CatalogProductMapper() {}

  public static CatalogProductResponse toResponse(CatalogProduct product) {
    return new CatalogProductResponse(
        product.getId(),
        product.getCategory().getId(),
        product.getName(),
        product.getDescription(),
        product.getBasePrice(),
        product.getImageUrl(),
        product.getSortOrder(),
        product.isActive(),
        product.isPaused());
  }
}
