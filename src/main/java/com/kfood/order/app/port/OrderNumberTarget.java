package com.kfood.order.app.port;

public interface OrderNumberTarget {

  String getOrderNumber();

  void assignOrderNumber(String orderNumber);

  String getStoreTimezone();
}
