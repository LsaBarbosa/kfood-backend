package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.order.app.port.OrderQueryPort;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GetPublicOrderByNumberUseCaseTest {

  private final OrderQueryPort orderQueryPort = mock(OrderQueryPort.class);
  private final GetPublicOrderByNumberUseCase useCase =
      new GetPublicOrderByNumberUseCase(orderQueryPort);

  @Test
  void shouldReturnPublicOrderWhenFound() {
    var output =
        new PublicOrderLookupOutput(
            "PED-20260326-000123",
            OrderStatus.NEW,
            PaymentStatusSnapshot.PENDING,
            FulfillmentType.DELIVERY,
            new BigDecimal("50.00"),
            new BigDecimal("6.50"),
            new BigDecimal("56.50"),
            Instant.parse("2026-03-26T12:00:00Z"),
            null);

    when(orderQueryPort.getPublicOrderLookup("loja-do-bairro", "PED-20260326-000123"))
        .thenReturn(output);

    var response = useCase.execute(" loja-do-bairro ", " PED-20260326-000123 ");

    assertThat(response.orderNumber()).isEqualTo("PED-20260326-000123");
    assertThat(response.status()).isEqualTo(OrderStatus.NEW);
    assertThat(response.paymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(response.fulfillmentType()).isEqualTo(FulfillmentType.DELIVERY);
    assertThat(response.totalAmount()).isEqualByComparingTo("56.50");
    assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-03-26T12:00:00Z"));
    assertThat(response.scheduledFor()).isNull();
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    when(orderQueryPort.getPublicOrderLookup("nao-existe", "PED-20260326-000123"))
        .thenThrow(new StoreSlugNotFoundException("nao-existe"));

    assertThatThrownBy(() -> useCase.execute(" nao-existe ", "PED-20260326-000123"))
        .isInstanceOf(StoreSlugNotFoundException.class);
  }

  @Test
  void shouldThrowWhenOrderDoesNotExist() {
    when(orderQueryPort.getPublicOrderLookup("loja-do-bairro", "PED-20260326-000999"))
        .thenThrow(new OrderNotFoundException("PED-20260326-000999"));

    assertThatThrownBy(() -> useCase.execute("loja-do-bairro", "PED-20260326-000999"))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found for number: PED-20260326-000999");
  }

  @Test
  void shouldThrowWhenOrderBelongsToAnotherStore() {
    when(orderQueryPort.getPublicOrderLookup("loja-do-bairro", "PED-20260326-000555"))
        .thenThrow(new OrderNotFoundException("PED-20260326-000555"));

    assertThatThrownBy(() -> useCase.execute("loja-do-bairro", "PED-20260326-000555"))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found for number: PED-20260326-000555");
  }
}
