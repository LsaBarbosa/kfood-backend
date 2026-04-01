package com.kfood.order.app;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import java.util.UUID;

public interface CheckoutCriticalValidationService {

  void revalidate(UUID storeId, CheckoutQuoteSnapshot quoteSnapshot);
}
