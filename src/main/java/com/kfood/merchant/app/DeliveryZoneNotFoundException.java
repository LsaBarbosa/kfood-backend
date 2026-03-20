package com.kfood.merchant.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class DeliveryZoneNotFoundException extends BusinessException {

  public DeliveryZoneNotFoundException(UUID zoneId) {
    super(
        ErrorCode.RESOURCE_NOT_FOUND,
        "Delivery zone not found for id: " + zoneId,
        HttpStatus.NOT_FOUND);
  }
}
