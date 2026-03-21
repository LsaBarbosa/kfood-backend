package com.kfood.catalog.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidCatalogProductAvailabilityException extends BusinessException {

  public InvalidCatalogProductAvailabilityException(String message) {
    super(ErrorCode.VALIDATION_ERROR, message, HttpStatus.BAD_REQUEST);
  }
}
