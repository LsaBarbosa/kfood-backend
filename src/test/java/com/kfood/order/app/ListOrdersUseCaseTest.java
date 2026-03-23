package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
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
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class ListOrdersUseCaseTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-22T15:00:00Z"), ZoneOffset.UTC);
  private final ListOrdersUseCase listOrdersUseCase =
      new ListOrdersUseCase(salesOrderRepository, currentTenantProvider, clock);

  @Test
  void shouldListOrdersByStatusForAuthenticatedTenant() {
    var storeId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
    var order = order(storeId, "PED-20260322-000123");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(salesOrderRepository.findOperationalQueue(
            storeId,
            OrderStatus.NEW,
            null,
            null,
            null,
            OffsetDateTime.parse("2026-03-22T15:00:00Z"),
            pageable))
        .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

    var response =
        listOrdersUseCase.execute(new ListOrdersQuery(OrderStatus.NEW, null, null, null), pageable);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().orderNumber()).isEqualTo("PED-20260322-000123");
    assertThat(response.items().getFirst().customerName()).isEqualTo("Lucas Santana");
    assertThat(response.items().getFirst().paymentStatus())
        .isEqualTo(order.getPaymentStatusSnapshot());
    assertThat(response.totalElements()).isEqualTo(1);
    assertThat(response.sort()).containsExactly("createdAt,desc");
    verify(currentTenantProvider).getRequiredStoreId();
  }

  @Test
  void shouldTranslateDateRangeToUtcBoundaries() {
    var storeId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(salesOrderRepository.findOperationalQueue(
            eq(storeId), isNull(), eq(FulfillmentType.DELIVERY), any(), any(), any(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    listOrdersUseCase.execute(
        new ListOrdersQuery(
            null,
            LocalDate.parse("2026-03-20"),
            LocalDate.parse("2026-03-22"),
            FulfillmentType.DELIVERY),
        pageable);

    var createdFromCaptor = ArgumentCaptor.forClass(Instant.class);
    var createdToExclusiveCaptor = ArgumentCaptor.forClass(Instant.class);

    verify(salesOrderRepository)
        .findOperationalQueue(
            eq(storeId),
            isNull(),
            eq(FulfillmentType.DELIVERY),
            createdFromCaptor.capture(),
            createdToExclusiveCaptor.capture(),
            eq(OffsetDateTime.parse("2026-03-22T15:00:00Z")),
            eq(pageable));

    assertThat(createdFromCaptor.getValue()).isEqualTo(Instant.parse("2026-03-20T00:00:00Z"));
    assertThat(createdToExclusiveCaptor.getValue())
        .isEqualTo(Instant.parse("2026-03-23T00:00:00Z"));
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

  @Test
  void shouldAllowOpenEndedDateRange() {
    var storeId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(salesOrderRepository.findOperationalQueue(
            eq(storeId), isNull(), isNull(), isNull(), any(), any(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var response =
        listOrdersUseCase.execute(
            new ListOrdersQuery(null, null, LocalDate.parse("2026-03-22"), null), pageable);

    assertThat(response.items()).isEmpty();
  }

  @Test
  void shouldAllowDateFromWithoutDateTo() {
    var storeId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(salesOrderRepository.findOperationalQueue(
            eq(storeId), isNull(), isNull(), any(), isNull(), any(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var response =
        listOrdersUseCase.execute(
            new ListOrdersQuery(null, LocalDate.parse("2026-03-22"), null, null), pageable);

    assertThat(response.items()).isEmpty();
  }

  @Test
  void shouldMapEmptySortWhenPageableHasNoOrdering() {
    var storeId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20, Sort.unsorted());

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(salesOrderRepository.findOperationalQueue(
            eq(storeId), isNull(), isNull(), isNull(), isNull(), any(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var response = listOrdersUseCase.execute(new ListOrdersQuery(null, null, null, null), pageable);

    assertThat(response.sort()).isEmpty();
  }

  private SalesOrder order(UUID storeId, String orderNumber) {
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var customer =
        new Customer(UUID.randomUUID(), store, "Lucas Santana", "21999990000", "lucas@email.com");
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            new BigDecimal("6.50"),
            new BigDecimal("56.50"),
            null,
            null);
    order.assignOrderNumber(orderNumber);
    return order;
  }
}
