package com.kfood.order.app;

import com.kfood.order.app.port.OrderWorkflowPort;
import com.kfood.order.domain.OrderStatus;
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
  OrderWorkflowPort.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class,
  Clock.class
})
public class UpdateOrderStatusUseCase {

  private final OrderWorkflowPort orderWorkflowPort;
  private final CurrentTenantProvider currentTenantProvider;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;
  private final Clock clock;

  public UpdateOrderStatusUseCase(
      OrderWorkflowPort orderWorkflowPort,
      CurrentTenantProvider currentTenantProvider,
      CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider,
      Clock clock) {
    this.orderWorkflowPort = orderWorkflowPort;
    this.currentTenantProvider = currentTenantProvider;
    this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
    this.clock = clock;
  }

  @Transactional
  public UpdateOrderStatusOutput execute(UUID orderId, UpdateOrderStatusCommand command) {
    Objects.requireNonNull(orderId, "orderId must not be null");
    Objects.requireNonNull(command, "command must not be null");

    var targetStatus = Objects.requireNonNull(command.newStatus(), "newStatus must not be null");
    if (targetStatus == OrderStatus.CANCELED) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Use the cancel endpoint to move an order to CANCELED.",
          HttpStatus.BAD_REQUEST);
    }

    var storeId = currentTenantProvider.getRequiredStoreId();
    var actorUserId = currentAuthenticatedUserProvider.getRequiredUserId();
    var changedAt = Instant.now(clock);
    return orderWorkflowPort.updateStatus(storeId, actorUserId, orderId, command, changedAt);
  }
}
