package com.kfood.merchant.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class StoreSlugNotFoundException extends BusinessException {

  public StoreSlugNotFoundException(String slug) {
    super(ErrorCode.RESOURCE_NOT_FOUND, "Store not found for slug: " + slug, HttpStatus.NOT_FOUND);
  }
}
