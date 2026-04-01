package com.kfood.order.app;

import com.kfood.order.app.port.OrderNumberTarget;

public interface OrderNumberGenerator {

  String next(OrderNumberTarget order);
}
