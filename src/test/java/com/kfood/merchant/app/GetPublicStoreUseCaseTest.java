package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetPublicStoreUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final GetPublicStoreUseCase getPublicStoreUseCase =
      new GetPublicStoreUseCase(
          storeRepository, storeBusinessHourRepository, deliveryZoneRepository);

  @Test
  void shouldReturnPublicStoreWithOnlyPublicData() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    store.activate();

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(storeBusinessHourRepository.findByStoreId(store.getId()))
        .thenReturn(
            List.of(
                StoreBusinessHour.open(
                    store, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(22, 0)),
                StoreBusinessHour.closed(store, DayOfWeek.MONDAY)));
    when(deliveryZoneRepository.findAllByStoreIdAndActiveTrueOrderByZoneNameAsc(store.getId()))
        .thenReturn(
            List.of(
                new DeliveryZone(
                    UUID.randomUUID(),
                    store,
                    "Centro",
                    new BigDecimal("6.50"),
                    new BigDecimal("25.00"),
                    true)));

    var response = getPublicStoreUseCase.execute(" loja-do-bairro ");

    assertThat(response.slug()).isEqualTo("loja-do-bairro");
    assertThat(response.name()).isEqualTo("Loja do Bairro");
    assertThat(response.phone()).isEqualTo("21999990000");
    assertThat(response.status().name()).isEqualTo("ACTIVE");
    assertThat(response.hours()).hasSize(2);
    assertThat(response.hours())
        .extracting(hour -> hour.dayOfWeek().name())
        .containsExactly("MONDAY", "TUESDAY");
    assertThat(response.deliveryZones()).hasSize(1);
    assertThat(response.deliveryZones().getFirst().zoneName()).isEqualTo("Centro");
    assertThat(response.deliveryZones().getFirst().feeAmount()).isEqualByComparingTo("6.50");
    assertThat(response.deliveryZones().getFirst().minOrderAmount()).isEqualByComparingTo("25.00");
  }

  @Test
  void shouldReturnNotFoundWhenSlugDoesNotExist() {
    when(storeRepository.findBySlug("loja-inexistente")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getPublicStoreUseCase.execute("loja-inexistente"))
        .isInstanceOf(StoreSlugNotFoundException.class)
        .hasMessageContaining("loja-inexistente");
  }
}
