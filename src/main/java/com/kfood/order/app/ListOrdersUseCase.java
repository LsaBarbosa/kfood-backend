package com.kfood.order.app;

import com.kfood.order.app.port.OrderQueryPort;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({OrderQueryPort.class, CurrentTenantProvider.class, Clock.class})
public class ListOrdersUseCase {

  private final OrderQueryPort orderQueryPort;
  private final CurrentTenantProvider currentTenantProvider;
  private final Clock clock;

  public ListOrdersUseCase(
      OrderQueryPort orderQueryPort, CurrentTenantProvider currentTenantProvider, Clock clock) {
    this.orderQueryPort = orderQueryPort;
    this.currentTenantProvider = currentTenantProvider;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ListOrdersOutput execute(ListOrdersQuery query, Pageable pageable) {
    validateDateRange(query.dateFrom(), query.dateTo());

    var storeId = currentTenantProvider.getRequiredStoreId();
    return orderQueryPort.listOperationalOrders(
        storeId, query, OffsetDateTime.now(clock), pageable);
  }

  private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
    if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "dateFrom must be less than or equal to dateTo.",
          HttpStatus.BAD_REQUEST);
    }
  }
}
