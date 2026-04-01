package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogOptionGroupResponse;
import com.kfood.catalog.api.CatalogOptionItemResponse;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;

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
        optionGroup.isActive(),
        optionGroup.getItems().stream().map(CatalogOptionGroupMapper::toItemResponse).toList());
  }

  private static CatalogOptionItemResponse toItemResponse(CatalogOptionItem optionItem) {
    return new CatalogOptionItemResponse(
        optionItem.getId(),
        optionItem.getName(),
        optionItem.getExtraPrice(),
        optionItem.isActive(),
        optionItem.getSortOrder());
  }
}
