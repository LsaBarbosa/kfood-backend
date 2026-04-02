package com.kfood.merchant.app;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeleteDeliveryZoneUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final DeleteDeliveryZoneUseCase deleteDeliveryZoneUseCase =
      new DeleteDeliveryZoneUseCase(merchantCommandPort, currentTenantProvider);

  @Test
  void shouldDeleteZoneForAuthenticatedTenant() {
    var storeId = UUID.randomUUID();
    var zoneId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);

    deleteDeliveryZoneUseCase.execute(zoneId);

    verify(merchantCommandPort).deleteDeliveryZone(storeId, zoneId);
  }
}
