package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.api.OrderDetailResponse;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderItem;
import com.kfood.order.infra.persistence.SalesOrderItemOption;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetOrderDetailUseCaseTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final GetOrderDetailUseCase useCase =
      new GetOrderDetailUseCase(salesOrderRepository, currentTenantProvider);

  @Test
  void shouldReturnCompleteOrderDetail() {
    var storeId = UUID.randomUUID();
    var order = newDeliveryOrder(storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(salesOrderRepository.findDetailedByIdAndStoreId(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    OrderDetailResponse response = useCase.execute(order.getId());

    assertThat(response.id()).isEqualTo(order.getId());
    assertThat(response.orderNumber()).isEqualTo("PED-20260322-000123");
    assertThat(response.status()).isEqualTo(OrderStatus.NEW);
    assertThat(response.payment().method()).isEqualTo(PaymentMethod.PIX);
    assertThat(response.payment().status()).isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(response.customer().name()).isEqualTo("Lucas Santana");
    assertThat(response.address()).isNotNull();
    assertThat(response.address().street()).isEqualTo("Rua das Flores");
    assertThat(response.address().number()).isEqualTo("45");
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().productNameSnapshot()).isEqualTo("Pizza Calabresa");
    assertThat(response.items().getFirst().options()).hasSize(1);
    assertThat(response.items().getFirst().options().getFirst().optionNameSnapshot())
        .isEqualTo("Borda Catupiry");
  }

  @Test
  void shouldReturnNullAddressForPickupOrder() {
    var storeId = UUID.randomUUID();
    var order = newPickupOrder(storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(salesOrderRepository.findDetailedByIdAndStoreId(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    var response = useCase.execute(order.getId());

    assertThat(response.address()).isNull();
  }

  @Test
  void shouldReturnNotFoundWhenOrderDoesNotExist() {
    var storeId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(salesOrderRepository.findDetailedByIdAndStoreId(orderId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(orderId))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable ->
                assertThat(((BusinessException) throwable).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }

  @Test
  void shouldNotExposeOrderFromAnotherTenant() {
    var authenticatedStoreId = UUID.randomUUID();
    var otherStoreOrderId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(authenticatedStoreId);
    when(salesOrderRepository.findDetailedByIdAndStoreId(otherStoreOrderId, authenticatedStoreId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(otherStoreOrderId))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable ->
                assertThat(((BusinessException) throwable).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private SalesOrder newDeliveryOrder(UUID storeId) {
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
    var address =
        new CustomerAddress(
            UUID.randomUUID(),
            customer,
            "Casa",
            "25000000",
            "Rua das Flores",
            "45",
            "Centro",
            "Mage",
            "RJ",
            "Ap 101",
            true);
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
            "Tocar campainha");

    order.assignOrderNumber("PED-20260322-000123");
    order.defineDeliveryAddressSnapshot(address);

    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            "Sem cebola");
    item.addOption(
        SalesOrderItemOption.create(
            UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 1));
    order.addItem(item);
    return order;
  }

  private SalesOrder newPickupOrder(UUID storeId) {
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
            FulfillmentType.PICKUP,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            BigDecimal.ZERO,
            new BigDecimal("50.00"),
            null,
            null);

    order.assignOrderNumber("PED-20260322-000124");
    return order;
  }
}
