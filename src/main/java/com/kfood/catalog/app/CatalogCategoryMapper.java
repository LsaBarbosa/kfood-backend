package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogCategoryResponse;
import com.kfood.catalog.infra.persistence.CatalogCategory;

public final class CatalogCategoryMapper {

  private CatalogCategoryMapper() {}

  public static CatalogCategoryResponse toResponse(CatalogCategory category) {
    return new CatalogCategoryResponse(
        category.getId(), category.getName(), category.getSortOrder(), category.isActive());
  }
}
