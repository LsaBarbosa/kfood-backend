package com.kfood.merchant.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class StoreSlugAlreadyExistsException extends BusinessException {

  public StoreSlugAlreadyExistsException(String slug) {
    super(ErrorCode.VALIDATION_ERROR, "Store slug already exists: " + slug, HttpStatus.CONFLICT);
  }
}
