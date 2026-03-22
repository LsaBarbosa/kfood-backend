package com.kfood.order.app;

import com.kfood.order.api.UpdateOrderStatusRequest;
import com.kfood.order.api.UpdateOrderStatusResponse;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.OrderStatusHistory;
import com.kfood.order.infra.persistence.OrderStatusHistoryRepository;
import com.kfood.order.infra.persistence.OrderStatusTransitionException;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  SalesOrderRepository.class,
  OrderStatusHistoryRepository.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class,
  Clock.class
})
public class UpdateOrderStatusUseCase {

  private final SalesOrderRepository salesOrderRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;
  private final Clock clock;

  public UpdateOrderStatusUseCase(
      SalesOrderRepository salesOrderRepository,
      OrderStatusHistoryRepository orderStatusHistoryRepository,
      CurrentTenantProvider currentTenantProvider,
      CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider,
      Clock clock) {
    this.salesOrderRepository = salesOrderRepository;
    this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
    this.clock = clock;
  }

  @Transactional
  public UpdateOrderStatusResponse execute(UUID orderId, UpdateOrderStatusRequest request) {
    Objects.requireNonNull(orderId, "orderId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    var targetStatus = Objects.requireNonNull(request.newStatus(), "newStatus must not be null");
    if (targetStatus == OrderStatus.CANCELED) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Use the cancel endpoint to move an order to CANCELED.",
          HttpStatus.BAD_REQUEST);
    }

    var storeId = currentTenantProvider.getRequiredStoreId();
    var actorUserId = currentAuthenticatedUserProvider.getRequiredUserId();
    var order =
        salesOrderRepository
            .findByIdAndStoreId(orderId, storeId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    var previousStatus = order.getStatus();

    try {
      order.changeStatus(targetStatus);
    } catch (OrderStatusTransitionException exception) {
      throw new BusinessException(
          ErrorCode.ORDER_STATUS_TRANSITION_INVALID, exception.getMessage(), HttpStatus.CONFLICT);
    }

    var changedAt = Instant.now(clock);

    salesOrderRepository.saveAndFlush(order);
    orderStatusHistoryRepository.saveAndFlush(
        OrderStatusHistory.create(
            UUID.randomUUID(),
            storeId,
            order.getId(),
            previousStatus,
            order.getStatus(),
            actorUserId,
            changedAt,
            request.reason()));

    return new UpdateOrderStatusResponse(
        order.getId(), previousStatus, order.getStatus(), changedAt, actorUserId);
  }
}
