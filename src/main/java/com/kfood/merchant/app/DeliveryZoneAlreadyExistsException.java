package com.kfood.merchant.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class DeliveryZoneAlreadyExistsException extends BusinessException {

  public DeliveryZoneAlreadyExistsException(String zoneName) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Delivery zone already exists: " + zoneName,
        HttpStatus.CONFLICT);
  }
}
