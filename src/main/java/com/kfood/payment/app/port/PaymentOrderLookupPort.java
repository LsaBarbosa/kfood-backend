package com.kfood.payment.app.port;

import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderLookupPort {

  Optional<PaymentOrder> findOrderById(UUID orderId);

  Optional<PaymentOrder> findOrderByIdAndStoreId(UUID orderId, UUID storeId);
}
