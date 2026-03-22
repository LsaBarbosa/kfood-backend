package com.kfood.order.app;

import com.kfood.order.infra.persistence.SalesOrder;

public interface OrderNumberGenerator {

  String next(SalesOrder order);
}
