package com.kfood.order.app;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.merchant.infra.persistence.Store;

public interface CheckoutCriticalValidationService {

  void revalidate(Store store, CheckoutQuoteSnapshot quoteSnapshot);
}
