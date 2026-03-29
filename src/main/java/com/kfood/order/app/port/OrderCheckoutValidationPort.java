package com.kfood.order.app.port;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import java.util.UUID;

public interface OrderCheckoutValidationPort {

  void revalidate(UUID storeId, CheckoutQuoteSnapshot quoteSnapshot);
}
