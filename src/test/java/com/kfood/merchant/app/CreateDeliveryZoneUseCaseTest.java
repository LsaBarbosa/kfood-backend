package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.api.CreateDeliveryZoneRequest;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateDeliveryZoneUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CreateDeliveryZoneUseCase createDeliveryZoneUseCase =
      new CreateDeliveryZoneUseCase(storeRepository, deliveryZoneRepository, currentTenantProvider);

  @Test
  void shouldCreateValidZone() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new CreateDeliveryZoneRequest(
            "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true);
    var savedZone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.existsByStoreIdAndZoneName(storeId, "Centro")).thenReturn(false);
    when(deliveryZoneRepository.saveAndFlush(any(DeliveryZone.class))).thenReturn(savedZone);

    var response = createDeliveryZoneUseCase.execute(request);

    assertThat(response.id()).isEqualTo(savedZone.getId());
    assertThat(response.zoneName()).isEqualTo("Centro");
    assertThat(response.feeAmount()).isEqualByComparingTo("6.50");
    assertThat(response.minOrderAmount()).isEqualByComparingTo("25.00");
    assertThat(response.active()).isTrue();
  }

  @Test
  void shouldRejectDuplicateZoneNameWithinSameStore() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new CreateDeliveryZoneRequest(
            "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.existsByStoreIdAndZoneName(storeId, "Centro")).thenReturn(true);

    assertThatThrownBy(() -> createDeliveryZoneUseCase.execute(request))
        .isInstanceOf(DeliveryZoneAlreadyExistsException.class)
        .hasMessageContaining("Centro");
  }
}
