package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.order.app.port.OrderQueryPort;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class ListOrdersUseCaseTest {

  private final OrderQueryPort orderQueryPort = mock(OrderQueryPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-22T15:00:00Z"), ZoneOffset.UTC);
  private final ListOrdersUseCase listOrdersUseCase =
      new ListOrdersUseCase(orderQueryPort, currentTenantProvider, clock);

  @Test
  void shouldListOrdersByStatusForAuthenticatedTenant() {
    var storeId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
    var query = new ListOrdersQuery(OrderStatus.NEW, null, null, null);
    var output =
        new ListOrdersOutput(
            List.of(
                new ListOrdersOutput.Item(
                    UUID.randomUUID(),
                    "PED-20260322-000123",
                    OrderStatus.NEW,
                    PaymentStatusSnapshot.PENDING,
                    "Lucas Santana",
                    new BigDecimal("56.50"),
                    Instant.parse("2026-03-22T15:00:00Z"))),
            0,
            20,
            1,
            1,
            List.of("createdAt,desc"));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(orderQueryPort.listOperationalOrders(
            storeId, query, OffsetDateTime.parse("2026-03-22T15:00:00Z"), pageable))
        .thenReturn(output);

    var response = listOrdersUseCase.execute(query, pageable);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().orderNumber()).isEqualTo("PED-20260322-000123");
    assertThat(response.items().getFirst().customerName()).isEqualTo("Lucas Santana");
    assertThat(response.items().getFirst().paymentStatusSnapshot())
        .isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(response.totalElements()).isEqualTo(1);
    assertThat(response.sort()).containsExactly("createdAt,desc");
    verify(currentTenantProvider).getRequiredStoreId();
  }

  @Test
  void shouldDelegateDateRangeQueryToPort() {
    var storeId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
    var query =
        new ListOrdersQuery(
            null,
            LocalDate.parse("2026-03-20"),
            LocalDate.parse("2026-03-22"),
            FulfillmentType.DELIVERY);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(orderQueryPort.listOperationalOrders(
            eq(storeId), eq(query), eq(OffsetDateTime.parse("2026-03-22T15:00:00Z")), eq(pageable)))
        .thenReturn(new ListOrdersOutput(List.of(), 0, 20, 0, 0, List.of("createdAt,desc")));

    listOrdersUseCase.execute(query, pageable);

    verify(orderQueryPort)
        .listOperationalOrders(
            storeId, query, OffsetDateTime.parse("2026-03-22T15:00:00Z"), pageable);
  }

  @Test
  void shouldRejectInvalidDateRange() {
    var throwable =
        catchThrowable(
            () ->
                listOrdersUseCase.execute(
                    new ListOrdersQuery(
                        null, LocalDate.parse("2026-03-23"), LocalDate.parse("2026-03-22"), null),
                    PageRequest.of(0, 20)));

    assertThat(throwable)
        .isInstanceOf(BusinessException.class)
        .hasMessage("dateFrom must be less than or equal to dateTo.");
    assertThat(((BusinessException) throwable).getErrorCode())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }
}
