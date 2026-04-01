package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.order.app.port.OrderQueryPort;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetOrderDetailUseCaseTest {

  private final OrderQueryPort orderQueryPort = mock(OrderQueryPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final GetOrderDetailUseCase useCase =
      new GetOrderDetailUseCase(orderQueryPort, currentTenantProvider);

  @Test
  void shouldReturnCompleteOrderDetail() {
    var storeId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    var responseOutput =
        new OrderDetailOutput(
            orderId,
            "PED-20260322-000123",
            OrderStatus.NEW,
            FulfillmentType.DELIVERY,
            new BigDecimal("50.00"),
            new BigDecimal("6.50"),
            new BigDecimal("56.50"),
            "Tocar campainha",
            null,
            Instant.parse("2026-03-22T15:00:00Z"),
            Instant.parse("2026-03-22T15:00:00Z"),
            new OrderDetailOutput.Customer(
                UUID.randomUUID(), "Lucas Santana", "21999990000", "lucas@email.com"),
            new OrderDetailOutput.Address(
                "Casa", "25000000", "Rua das Flores", "45", "Centro", "Mage", "RJ", "Ap 101"),
            new OrderDetailOutput.Payment(PaymentMethod.PIX, PaymentStatusSnapshot.PENDING),
            List.of(
                new OrderDetailOutput.Item(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Pizza Calabresa",
                    new BigDecimal("42.00"),
                    1,
                    new BigDecimal("50.00"),
                    "Sem cebola",
                    List.of(
                        new OrderDetailOutput.ItemOption(
                            UUID.randomUUID(),
                            "Borda Catupiry",
                            new BigDecimal("8.00"),
                            1,
                            new BigDecimal("8.00"))))));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(orderQueryPort.getOrderDetail(storeId, orderId)).thenReturn(responseOutput);

    var response = useCase.execute(orderId);

    assertThat(response.id()).isEqualTo(orderId);
    assertThat(response.orderNumber()).isEqualTo("PED-20260322-000123");
    assertThat(response.status()).isEqualTo(OrderStatus.NEW);
    assertThat(response.payment().paymentMethodSnapshot()).isEqualTo(PaymentMethod.PIX);
    assertThat(response.payment().paymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
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
    var orderId = UUID.randomUUID();
    var responseOutput =
        new OrderDetailOutput(
            orderId,
            "PED-20260322-000124",
            OrderStatus.NEW,
            FulfillmentType.PICKUP,
            new BigDecimal("50.00"),
            BigDecimal.ZERO,
            new BigDecimal("50.00"),
            null,
            null,
            Instant.parse("2026-03-22T15:00:00Z"),
            Instant.parse("2026-03-22T15:00:00Z"),
            new OrderDetailOutput.Customer(
                UUID.randomUUID(), "Lucas Santana", "21999990000", "lucas@email.com"),
            null,
            new OrderDetailOutput.Payment(PaymentMethod.PIX, PaymentStatusSnapshot.PENDING),
            List.of());

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(orderQueryPort.getOrderDetail(storeId, orderId)).thenReturn(responseOutput);

    var response = useCase.execute(orderId);

    assertThat(response.address()).isNull();
  }

  @Test
  void shouldReturnNotFoundWhenOrderDoesNotExist() {
    var storeId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(orderQueryPort.getOrderDetail(storeId, orderId))
        .thenThrow(new OrderNotFoundException(orderId));

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
    when(orderQueryPort.getOrderDetail(authenticatedStoreId, otherStoreOrderId))
        .thenThrow(new OrderNotFoundException(otherStoreOrderId));

    assertThatThrownBy(() -> useCase.execute(otherStoreOrderId))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable ->
                assertThat(((BusinessException) throwable).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
