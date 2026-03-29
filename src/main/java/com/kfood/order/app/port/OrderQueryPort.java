package com.kfood.order.app.port;

import com.kfood.order.app.ListOrdersOutput;
import com.kfood.order.app.ListOrdersQuery;
import com.kfood.order.app.OrderDetailOutput;
import com.kfood.order.app.PublicOrderLookupOutput;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface OrderQueryPort {

  ListOrdersOutput listOperationalOrders(
      UUID storeId, ListOrdersQuery query, OffsetDateTime now, Pageable pageable);

  OrderDetailOutput getOrderDetail(UUID storeId, UUID orderId);

  PublicOrderLookupOutput getPublicOrderLookup(String slug, String orderNumber);
}
