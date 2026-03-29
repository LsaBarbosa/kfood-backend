package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListDeliveryZonesUseCaseTest {

  private final MerchantQueryPort merchantQueryPort = mock(MerchantQueryPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final ListDeliveryZonesUseCase listDeliveryZonesUseCase =
      new ListDeliveryZonesUseCase(merchantQueryPort, currentTenantProvider);

  @Test
  void shouldListZonesForAuthenticatedTenant() {
    var storeId = UUID.randomUUID();
    var output =
        List.of(
            new DeliveryZoneOutput(
                UUID.randomUUID(), "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true),
            new DeliveryZoneOutput(
                UUID.randomUUID(),
                "Zona Sul",
                new BigDecimal("8.00"),
                new BigDecimal("30.00"),
                false));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantQueryPort.listDeliveryZones(storeId)).thenReturn(output);

    var response = listDeliveryZonesUseCase.execute();

    assertThat(response).hasSize(2);
    assertThat(response.getFirst().zoneName()).isEqualTo("Centro");
    assertThat(response.getLast().zoneName()).isEqualTo("Zona Sul");
    verify(merchantQueryPort).listDeliveryZones(storeId);
  }
}
