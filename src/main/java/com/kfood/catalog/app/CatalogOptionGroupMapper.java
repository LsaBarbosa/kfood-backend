package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogOptionGroupResponse;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;

public final class CatalogOptionGroupMapper {

  private CatalogOptionGroupMapper() {}

  public static CatalogOptionGroupResponse toResponse(CatalogOptionGroup optionGroup) {
    return new CatalogOptionGroupResponse(
        optionGroup.getId(),
        optionGroup.getProduct().getId(),
        optionGroup.getName(),
        optionGroup.getMinSelect(),
        optionGroup.getMaxSelect(),
        optionGroup.isRequired(),
        optionGroup.isActive());
  }
}
