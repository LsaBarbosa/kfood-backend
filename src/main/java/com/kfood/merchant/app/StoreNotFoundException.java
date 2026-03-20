package com.kfood.merchant.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class StoreNotFoundException extends BusinessException {

  public StoreNotFoundException(UUID storeId) {
    super(ErrorCode.RESOURCE_NOT_FOUND, "Store not found for id: " + storeId, HttpStatus.NOT_FOUND);
  }
}
