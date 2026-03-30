package com.kfood.order.infra.adapter;

import com.kfood.order.app.CancelOrderCommand;
import com.kfood.order.app.CancelOrderOutput;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.app.UpdateOrderStatusCommand;
import com.kfood.order.app.UpdateOrderStatusOutput;
import com.kfood.order.app.port.OrderWorkflowPort;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.domain.OrderStatusTransitionException;
import com.kfood.order.infra.persistence.OrderStatusHistory;
import com.kfood.order.infra.persistence.OrderStatusHistoryRepository;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.shared.exceptions.ApiFieldError;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JpaOrderWorkflowAdapter implements OrderWorkflowPort {

  private final SalesOrderRepository salesOrderRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;

  public JpaOrderWorkflowAdapter(
      SalesOrderRepository salesOrderRepository,
      OrderStatusHistoryRepository orderStatusHistoryRepository) {
    this.salesOrderRepository = salesOrderRepository;
    this.orderStatusHistoryRepository = orderStatusHistoryRepository;
  }

  @Override
  public UpdateOrderStatusOutput updateStatus(
      UUID storeId,
      UUID actorUserId,
      UUID orderId,
      UpdateOrderStatusCommand command,
      Instant changedAt) {
    var targetStatus = Objects.requireNonNull(command.newStatus(), "newStatus must not be null");
    if (targetStatus == OrderStatus.CANCELED) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Use the cancel endpoint to move an order to CANCELED.",
          HttpStatus.BAD_REQUEST);
    }

    var order =
        salesOrderRepository
            .findByIdAndStore_Id(orderId, storeId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    var previousStatus = order.getStatus();

    try {
      order.changeStatus(targetStatus);
    } catch (OrderStatusTransitionException exception) {
      throw new BusinessException(
          ErrorCode.ORDER_STATUS_TRANSITION_INVALID, exception.getMessage(), HttpStatus.CONFLICT);
    }

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
            command.reason()));

    return new UpdateOrderStatusOutput(
        order.getId(), previousStatus, order.getStatus(), changedAt, actorUserId);
  }

  @Override
  public CancelOrderOutput cancel(
      UUID storeId,
      UUID actorUserId,
      UUID orderId,
      CancelOrderCommand command,
      Instant canceledAt) {
    var reason = normalizeRequiredReason(command.reason());
    var order =
        salesOrderRepository
            .findByIdAndStore_Id(orderId, storeId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    var previousStatus = order.getStatus();

    try {
      order.cancel();
    } catch (OrderStatusTransitionException exception) {
      throw new BusinessException(
          ErrorCode.ORDER_STATUS_TRANSITION_INVALID, exception.getMessage(), HttpStatus.CONFLICT);
    }

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
