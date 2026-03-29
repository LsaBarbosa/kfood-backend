package com.kfood.order.app;

import com.kfood.order.app.port.OrderWorkflowPort;
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
  OrderWorkflowPort.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class,
  Clock.class
})
public class CancelOrderUseCase {

  private final OrderWorkflowPort orderWorkflowPort;
  private final CurrentTenantProvider currentTenantProvider;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;
  private final Clock clock;

  public CancelOrderUseCase(
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
  public CancelOrderOutput execute(UUID orderId, CancelOrderCommand command) {
    Objects.requireNonNull(orderId, "orderId must not be null");
    Objects.requireNonNull(command, "command must not be null");

    var reason = normalizeRequiredReason(command.reason());
    var storeId = currentTenantProvider.getRequiredStoreId();
    var actorUserId = currentAuthenticatedUserProvider.getRequiredUserId();
    var canceledAt = Instant.now(clock);
    return orderWorkflowPort.cancel(
        storeId, actorUserId, orderId, new CancelOrderCommand(reason), canceledAt);
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
