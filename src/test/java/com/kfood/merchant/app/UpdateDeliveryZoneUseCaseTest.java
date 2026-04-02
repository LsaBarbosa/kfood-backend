package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateDeliveryZoneUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final UpdateDeliveryZoneUseCase updateDeliveryZoneUseCase =
      new UpdateDeliveryZoneUseCase(merchantCommandPort, currentTenantProvider);

  @Test
  void shouldUpdateZoneForAuthenticatedTenant() {
    var storeId = UUID.randomUUID();
    var zoneId = UUID.randomUUID();
    var command =
        new UpdateDeliveryZoneCommand(
            "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), false);
    var output =
        new DeliveryZoneOutput(
            zoneId, "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantCommandPort.updateDeliveryZone(storeId, zoneId, command)).thenReturn(output);

    var response = updateDeliveryZoneUseCase.execute(zoneId, command);

    assertThat(response.active()).isFalse();
    verify(merchantCommandPort).updateDeliveryZone(storeId, zoneId, command);
  }
}
