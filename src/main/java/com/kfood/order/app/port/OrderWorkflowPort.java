package com.kfood.order.app.port;

import com.kfood.order.app.CancelOrderCommand;
import com.kfood.order.app.CancelOrderOutput;
import com.kfood.order.app.UpdateOrderStatusCommand;
import com.kfood.order.app.UpdateOrderStatusOutput;
import java.time.Instant;
import java.util.UUID;

public interface OrderWorkflowPort {

  UpdateOrderStatusOutput updateStatus(
      UUID storeId,
      UUID actorUserId,
      UUID orderId,
      UpdateOrderStatusCommand command,
      Instant changedAt);

  CancelOrderOutput cancel(
      UUID storeId, UUID actorUserId, UUID orderId, CancelOrderCommand command, Instant canceledAt);
}
