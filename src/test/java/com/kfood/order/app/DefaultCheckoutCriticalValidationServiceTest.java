package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.order.app.port.OrderCheckoutValidationPort;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class DefaultCheckoutCriticalValidationServiceTest {

  @Test
  void shouldDelegateRevalidationToPort() {
    var validationPort = mock(OrderCheckoutValidationPort.class);
    var service = new DefaultCheckoutCriticalValidationService(providerOf(validationPort));
    var storeId = UUID.randomUUID();
    var quote =
        new CheckoutQuoteSnapshot(
            UUID.randomUUID(),
            storeId,
            UUID.randomUUID(),
            com.kfood.order.domain.FulfillmentType.PICKUP,
            null,
            new BigDecimal("40.00"),
            BigDecimal.ZERO,
            new BigDecimal("40.00"),
            List.of(),
            OffsetDateTime.now().plusMinutes(10));

    service.revalidate(storeId, quote);

    verify(validationPort).revalidate(storeId, quote);
  }

  @Test
  void shouldFailWhenValidationPortIsUnavailable() {
    var service = new DefaultCheckoutCriticalValidationService(providerOf(null));
    var storeId = UUID.randomUUID();
    var quote =
        new CheckoutQuoteSnapshot(
            UUID.randomUUID(),
            storeId,
            UUID.randomUUID(),
            com.kfood.order.domain.FulfillmentType.PICKUP,
            null,
            new BigDecimal("40.00"),
            BigDecimal.ZERO,
            new BigDecimal("40.00"),
            List.of(),
            OffsetDateTime.now().plusMinutes(10));

    assertThatThrownBy(() -> service.revalidate(storeId, quote))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("OrderCheckoutValidationPort is not available.");
  }

  private ObjectProvider<OrderCheckoutValidationPort> providerOf(OrderCheckoutValidationPort port) {
    return new ObjectProvider<>() {
      @Override
      public OrderCheckoutValidationPort getObject(Object... args) {
        if (port == null) {
          throw new IllegalStateException("OrderCheckoutValidationPort is not available.");
        }
        return port;
      }

      @Override
      public OrderCheckoutValidationPort getIfAvailable() {
        return port;
      }

      @Override
      public OrderCheckoutValidationPort getIfUnique() {
        return port;
      }
    };
  }
}
