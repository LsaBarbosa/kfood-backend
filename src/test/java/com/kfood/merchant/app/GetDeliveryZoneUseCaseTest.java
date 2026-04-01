package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDeliveryZoneUseCaseTest {

  private final MerchantQueryPort merchantQueryPort = mock(MerchantQueryPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final GetDeliveryZoneUseCase getDeliveryZoneUseCase =
      new GetDeliveryZoneUseCase(merchantQueryPort, currentTenantProvider);

  @Test
  void shouldReturnCorrectFeeForZone() {
    var storeId = UUID.randomUUID();
    var zoneId = UUID.randomUUID();
    var output =
        new DeliveryZoneOutput(
            zoneId, "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantQueryPort.getDeliveryZone(storeId, zoneId)).thenReturn(output);

    var response = getDeliveryZoneUseCase.execute(zoneId);

    assertThat(response.id()).isEqualTo(zoneId);
    assertThat(response.zoneName()).isEqualTo("Centro");
    assertThat(response.feeAmount()).isEqualByComparingTo("6.50");
  }

  @Test
  void shouldThrowWhenZoneDoesNotExist() {
    var storeId = UUID.randomUUID();
    var zoneId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantQueryPort.getDeliveryZone(storeId, zoneId))
        .thenThrow(new DeliveryZoneNotFoundException(zoneId));

    assertThatThrownBy(() -> getDeliveryZoneUseCase.execute(zoneId))
        .isInstanceOf(DeliveryZoneNotFoundException.class)
        .hasMessageContaining(zoneId.toString());
  }
}
