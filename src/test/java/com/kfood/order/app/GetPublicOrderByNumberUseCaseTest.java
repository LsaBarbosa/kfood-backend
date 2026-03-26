package com.kfood.order.app;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.api.PublicOrderLookupResponse;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetPublicOrderByNumberUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final GetPublicOrderByNumberUseCase useCase =
      new GetPublicOrderByNumberUseCase(storeRepository, salesOrderRepository);

  @Test
  void shouldReturnPublicOrderWhenFound() {
    var store = store();
    var order = order(store);

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(salesOrderRepository.findByStoreIdAndOrderNumber(store.getId(), "PED-20260326-000123"))
        .thenReturn(Optional.of(order));

    PublicOrderLookupResponse response =
        useCase.execute(" loja-do-bairro ", " PED-20260326-000123 ");

    assertThat(response.orderNumber()).isEqualTo("PED-20260326-000123");
    assertThat(response.status()).isEqualTo(OrderStatus.NEW);
    assertThat(response.paymentStatus()).isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(response.fulfillmentType()).isEqualTo(FulfillmentType.DELIVERY);
    assertThat(response.totalAmount()).isEqualByComparingTo("56.50");
    assertThat(response.createdAt()).isEqualTo(order.getCreatedAt());
    assertThat(response.scheduledFor()).isNull();
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    when(storeRepository.findBySlug("nao-existe")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(" nao-existe ", "PED-20260326-000123"))
        .isInstanceOf(StoreSlugNotFoundException.class);
  }

  @Test
  void shouldThrowWhenOrderDoesNotExist() {
    var store = store();
    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(salesOrderRepository.findByStoreIdAndOrderNumber(store.getId(), "PED-20260326-000999"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute("loja-do-bairro", "PED-20260326-000999"))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found for number: PED-20260326-000999");
  }

  @Test
  void shouldThrowWhenOrderBelongsToAnotherStore() {
    var store = store();
    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(salesOrderRepository.findByStoreIdAndOrderNumber(store.getId(), "PED-20260326-000555"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute("loja-do-bairro", "PED-20260326-000555"))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found for number: PED-20260326-000555");
  }

  private Store store() {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }

  private SalesOrder order(Store store) {
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
            "Tocar campainha");
    order.assignOrderNumber("PED-20260326-000123");
    return order;
  }
}
