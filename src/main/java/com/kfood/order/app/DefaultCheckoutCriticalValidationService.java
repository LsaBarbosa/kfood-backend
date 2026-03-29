package com.kfood.order.app;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.order.app.port.OrderCheckoutValidationPort;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class DefaultCheckoutCriticalValidationService implements CheckoutCriticalValidationService {

  private final ObjectProvider<OrderCheckoutValidationPort> orderCheckoutValidationPortProvider;

  public DefaultCheckoutCriticalValidationService(
      ObjectProvider<OrderCheckoutValidationPort> orderCheckoutValidationPortProvider) {
    this.orderCheckoutValidationPortProvider = orderCheckoutValidationPortProvider;
  }

  @Override
  public void revalidate(UUID storeId, CheckoutQuoteSnapshot quoteSnapshot) {
    var orderCheckoutValidationPort =
        orderCheckoutValidationPortProvider.getIfAvailable(() -> null);
    if (orderCheckoutValidationPort == null) {
      throw new IllegalStateException("OrderCheckoutValidationPort is not available.");
    }
    orderCheckoutValidationPort.revalidate(storeId, quoteSnapshot);
  }
}
