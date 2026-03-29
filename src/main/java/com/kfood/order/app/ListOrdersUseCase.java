package com.kfood.order.app;

import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({SalesOrderRepository.class, CurrentTenantProvider.class, Clock.class})
public class ListOrdersUseCase {

  private final SalesOrderRepository salesOrderRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final Clock clock;

  public ListOrdersUseCase(
      SalesOrderRepository salesOrderRepository,
      CurrentTenantProvider currentTenantProvider,
      Clock clock) {
    this.salesOrderRepository = salesOrderRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ListOrdersOutput execute(ListOrdersQuery query, Pageable pageable) {
    validateDateRange(query.dateFrom(), query.dateTo());

    var storeId = currentTenantProvider.getRequiredStoreId();
    var page =
        salesOrderRepository.findOperationalQueue(
            storeId,
            query.status(),
            query.fulfillmentType(),
            toStartOfDay(query.dateFrom()),
            toStartOfNextDay(query.dateTo()),
            OffsetDateTime.now(clock),
            pageable);

    return new ListOrdersOutput(
        page.getContent().stream().map(this::toItem).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        mapSort(pageable));
  }

  private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
    if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "dateFrom must be less than or equal to dateTo.",
          HttpStatus.BAD_REQUEST);
    }
  }

  private Instant toStartOfDay(LocalDate value) {
    return value == null ? null : value.atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  private Instant toStartOfNextDay(LocalDate value) {
    return value == null ? null : value.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  private ListOrdersOutput.Item toItem(SalesOrder order) {
    return new ListOrdersOutput.Item(
        order.getId(),
        order.getOrderNumber(),
        order.getStatus(),
        order.getPaymentStatusSnapshot(),
        order.getCustomer().getName(),
        order.getTotalAmount(),
        order.getCreatedAt());
  }

  private List<String> mapSort(Pageable pageable) {
    return pageable.getSort().stream()
        .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
        .toList();
  }
}
