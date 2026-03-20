package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDeliveryZoneUseCaseTest {

  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final GetDeliveryZoneUseCase getDeliveryZoneUseCase =
      new GetDeliveryZoneUseCase(deliveryZoneRepository, currentTenantProvider);

  @Test
  void shouldReturnCorrectFeeForZone() {
    var storeId = UUID.randomUUID();
    var zoneId = UUID.randomUUID();
    var zone =
        new DeliveryZone(
            zoneId,
            new Store(
                storeId,
                "Loja do Bairro",
                "loja-do-bairro",
                "45.723.174/0001-10",
                "21999990000",
                "America/Sao_Paulo"),
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(deliveryZoneRepository.findByIdAndStoreId(zoneId, storeId)).thenReturn(Optional.of(zone));

    var response = getDeliveryZoneUseCase.execute(zoneId);

    assertThat(response.id()).isEqualTo(zoneId);
    assertThat(response.zoneName()).isEqualTo("Centro");
    assertThat(response.feeAmount()).isEqualByComparingTo("6.50");
    assertThat(response.minOrderAmount()).isEqualByComparingTo("25.00");
  }

  @Test
  void shouldThrowWhenZoneDoesNotExist() {
    var storeId = UUID.randomUUID();
    var zoneId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(deliveryZoneRepository.findByIdAndStoreId(zoneId, storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getDeliveryZoneUseCase.execute(zoneId))
        .isInstanceOf(DeliveryZoneNotFoundException.class)
        .hasMessageContaining(zoneId.toString());
  }
}
