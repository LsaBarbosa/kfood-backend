package com.kfood.payment.app.port;

import com.kfood.order.infra.persistence.SalesOrder;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderLookupPort {

  Optional<SalesOrder> findOrderById(UUID orderId);

  Optional<SalesOrder> findOrderByIdAndStoreId(UUID orderId, UUID storeId);
}
