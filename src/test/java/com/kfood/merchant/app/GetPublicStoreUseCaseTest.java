package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.merchant.domain.StoreStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetPublicStoreUseCaseTest {

  private final MerchantQueryPort merchantQueryPort = mock(MerchantQueryPort.class);
  private final GetPublicStoreUseCase getPublicStoreUseCase =
      new GetPublicStoreUseCase(merchantQueryPort);

  @Test
  void shouldReturnPublicStoreWithOnlyPublicData() {
    var output =
        new PublicStoreOutput(
            "loja-do-bairro",
            "Loja do Bairro",
            StoreStatus.ACTIVE,
            "21999990000",
            List.of(
                new PublicStoreHourOutput(DayOfWeek.MONDAY, null, null, true),
                new PublicStoreHourOutput(
                    DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false)),
            List.of(
                new PublicDeliveryZoneOutput(
                    "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"))));

    when(merchantQueryPort.getPublicStore("loja-do-bairro")).thenReturn(output);

    var response = getPublicStoreUseCase.execute(" loja-do-bairro ");

    assertThat(response.slug()).isEqualTo("loja-do-bairro");
    assertThat(response.hours()).hasSize(2);
    assertThat(response.deliveryZones().getFirst().zoneName()).isEqualTo("Centro");
  }

  @Test
  void shouldReturnNotFoundWhenSlugDoesNotExist() {
    when(merchantQueryPort.getPublicStore("loja-inexistente"))
        .thenThrow(new StoreSlugNotFoundException("loja-inexistente"));

    assertThatThrownBy(() -> getPublicStoreUseCase.execute("loja-inexistente"))
        .isInstanceOf(StoreSlugNotFoundException.class)
        .hasMessageContaining("loja-inexistente");
  }
}
