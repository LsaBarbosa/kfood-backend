package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateDeliveryZoneUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CreateDeliveryZoneUseCase createDeliveryZoneUseCase =
      new CreateDeliveryZoneUseCase(merchantCommandPort, currentTenantProvider);

  @Test
  void shouldCreateValidZone() {
    var storeId = UUID.randomUUID();
    var command =
        new CreateDeliveryZoneCommand(
            "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true);
    var output =
        new DeliveryZoneOutput(
            UUID.randomUUID(), "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantCommandPort.createDeliveryZone(storeId, command)).thenReturn(output);

    var response = createDeliveryZoneUseCase.execute(command);

    assertThat(response.zoneName()).isEqualTo("Centro");
    assertThat(response.feeAmount()).isEqualByComparingTo("6.50");
    assertThat(response.minOrderAmount()).isEqualByComparingTo("25.00");
    verify(merchantCommandPort).createDeliveryZone(storeId, command);
  }

  @Test
  void shouldPropagateDuplicateZoneNameError() {
    var storeId = UUID.randomUUID();
    var command =
        new CreateDeliveryZoneCommand(
            "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantCommandPort.createDeliveryZone(storeId, command))
        .thenThrow(new DeliveryZoneAlreadyExistsException("Centro"));

    assertThatThrownBy(() -> createDeliveryZoneUseCase.execute(command))
        .isInstanceOf(DeliveryZoneAlreadyExistsException.class)
        .hasMessageContaining("Centro");
  }
}
