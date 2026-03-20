package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListDeliveryZonesUseCaseTest {

  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final ListDeliveryZonesUseCase listDeliveryZonesUseCase =
      new ListDeliveryZonesUseCase(deliveryZoneRepository, currentTenantProvider);

  @Test
  void shouldListZonesOrderedFromRepository() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(deliveryZoneRepository.findAllByStoreIdOrderByZoneNameAsc(storeId))
        .thenReturn(
            List.of(
                new DeliveryZone(
                    UUID.randomUUID(),
                    store,
                    "Centro",
                    new BigDecimal("6.50"),
                    new BigDecimal("25.00"),
                    true),
                new DeliveryZone(
                    UUID.randomUUID(),
                    store,
                    "Zona Sul",
                    new BigDecimal("8.00"),
                    new BigDecimal("30.00"),
                    false)));

    var response = listDeliveryZonesUseCase.execute();

    assertThat(response).hasSize(2);
    assertThat(response.getFirst().zoneName()).isEqualTo("Centro");
    assertThat(response.getLast().zoneName()).isEqualTo("Zona Sul");
  }
}
