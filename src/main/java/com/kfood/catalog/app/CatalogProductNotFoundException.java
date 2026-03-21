package com.kfood.catalog.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CatalogProductNotFoundException extends BusinessException {

  public CatalogProductNotFoundException(UUID productId) {
    super(
        ErrorCode.RESOURCE_NOT_FOUND,
        "Catalog product not found for id: " + productId,
        HttpStatus.NOT_FOUND);
  }
}
