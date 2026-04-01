package com.kfood.merchant.infra.persistence;

import com.kfood.merchant.domain.StoreStatus;

public class StoreStatusTransitionException extends RuntimeException {

  public StoreStatusTransitionException(StoreStatus currentStatus, StoreStatus targetStatus) {
    super("Invalid store status transition from " + currentStatus + " to " + targetStatus);
  }
}
