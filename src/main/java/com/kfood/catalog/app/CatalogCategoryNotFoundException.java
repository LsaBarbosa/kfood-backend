package com.kfood.catalog.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CatalogCategoryNotFoundException extends BusinessException {

  public CatalogCategoryNotFoundException(UUID categoryId) {
    super(
        ErrorCode.RESOURCE_NOT_FOUND,
        "Catalog category not found for id: " + categoryId,
        HttpStatus.NOT_FOUND);
  }
}
