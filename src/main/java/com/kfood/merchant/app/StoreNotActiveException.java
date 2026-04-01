package com.kfood.merchant.app;

import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class StoreNotActiveException extends BusinessException {

  public StoreNotActiveException(UUID storeId, StoreStatus currentStatus) {
    super(
        ErrorCode.STORE_NOT_ACTIVE,
        "Store is not active. storeId=" + storeId + ", currentStatus=" + currentStatus,
        HttpStatus.CONFLICT);
  }
}
