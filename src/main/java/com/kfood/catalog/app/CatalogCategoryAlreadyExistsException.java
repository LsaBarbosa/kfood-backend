package com.kfood.catalog.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class CatalogCategoryAlreadyExistsException extends BusinessException {

  public CatalogCategoryAlreadyExistsException(String name) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Catalog category already exists: " + name,
        HttpStatus.CONFLICT);
  }
}
