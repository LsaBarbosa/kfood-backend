package com.kfood.order.app;

import com.kfood.order.app.port.OrderQueryPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({OrderQueryPort.class, CurrentTenantProvider.class})
public class GetOrderDetailUseCase {

  private final OrderQueryPort orderQueryPort;
  private final CurrentTenantProvider currentTenantProvider;

  public GetOrderDetailUseCase(
      OrderQueryPort orderQueryPort, CurrentTenantProvider currentTenantProvider) {
    this.orderQueryPort = orderQueryPort;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public OrderDetailOutput execute(UUID orderId) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    return orderQueryPort.getOrderDetail(storeId, orderId);
  }
}
