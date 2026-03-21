package com.kfood.merchant.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class OwnerAlreadyBoundToAnotherStoreException extends BusinessException {

  public OwnerAlreadyBoundToAnotherStoreException(UUID currentStoreId) {
    super(
        ErrorCode.TENANT_ACCESS_DENIED,
        "Owner is already bound to another store: " + currentStoreId,
        HttpStatus.FORBIDDEN);
  }
}
