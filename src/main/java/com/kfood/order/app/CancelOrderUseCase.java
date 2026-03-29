package com.kfood.order.app;

import com.kfood.order.domain.OrderStatusTransitionException;
import com.kfood.order.infra.persistence.OrderStatusHistory;
import com.kfood.order.infra.persistence.OrderStatusHistoryRepository;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.shared.exceptions.ApiFieldError;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
public class CancelOrderUseCase {

  private final SalesOrderRepository salesOrderRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;
  private final Clock clock;

  public CancelOrderUseCase(
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
  public CancelOrderOutput execute(UUID orderId, CancelOrderCommand command) {
    Objects.requireNonNull(orderId, "orderId must not be null");
    Objects.requireNonNull(command, "command must not be null");

    var reason = normalizeRequiredReason(command.reason());
    var storeId = currentTenantProvider.getRequiredStoreId();
    var actorUserId = currentAuthenticatedUserProvider.getRequiredUserId();
    var order =
        salesOrderRepository
            .findByIdAndStoreId(orderId, storeId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    var previousStatus = order.getStatus();

    try {
      order.cancel();
    } catch (OrderStatusTransitionException exception) {
      throw new BusinessException(
          ErrorCode.ORDER_STATUS_TRANSITION_INVALID, exception.getMessage(), HttpStatus.CONFLICT);
    }

    var canceledAt = Instant.now(clock);

    salesOrderRepository.saveAndFlush(order);
    orderStatusHistoryRepository.saveAndFlush(
        OrderStatusHistory.create(
            UUID.randomUUID(),
            storeId,
            order.getId(),
            previousStatus,
            order.getStatus(),
            actorUserId,
            canceledAt,
            reason));

    return new CancelOrderOutput(order.getId(), order.getStatus(), canceledAt, reason);
  }

  private String normalizeRequiredReason(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Cancellation reason must not be blank.",
          HttpStatus.BAD_REQUEST,
          List.of(new ApiFieldError("reason", "must not be blank")));
    }

    return value.trim();
  }
}
