package com.kfood.order.app;

import com.kfood.order.infra.persistence.SalesOrder;

public interface OrderPaymentRegistrar {

  void registerInitialPayment(SalesOrder order);
}
