package com.kfood.order.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BusinessException {

  public OrderNotFoundException(UUID orderId) {
    super(ErrorCode.RESOURCE_NOT_FOUND, "Order not found for id: " + orderId, HttpStatus.NOT_FOUND);
  }

  public OrderNotFoundException(String orderNumber) {
    super(
        ErrorCode.RESOURCE_NOT_FOUND,
        "Order not found for number: " + orderNumber,
        HttpStatus.NOT_FOUND);
  }
}
