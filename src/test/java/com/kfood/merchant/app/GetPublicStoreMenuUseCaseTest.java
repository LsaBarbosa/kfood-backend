package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantQueryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetPublicStoreMenuUseCaseTest {

  private final MerchantQueryPort merchantQueryPort = mock(MerchantQueryPort.class);
  private final GetPublicStoreMenuUseCase getPublicStoreMenuUseCase =
      new GetPublicStoreMenuUseCase(merchantQueryPort);

  @Test
  void shouldReturnOnlyVisibleOrderedMenuForStore() {
    var output =
        new PublicStoreMenuOutput(
            List.of(
                new PublicStoreMenuCategoryOutput(
                    UUID.randomUUID(),
                    "Bebidas",
                    List.of(
                        new PublicStoreMenuProductOutput(
                            UUID.randomUUID(),
                            "Refrigerante",
                            "Lata 350ml",
                            new BigDecimal("7.50"),
                            null,
                            false,
                            List.of()))),
                new PublicStoreMenuCategoryOutput(
                    UUID.randomUUID(),
                    "Pizzas",
                    List.of(
                        new PublicStoreMenuProductOutput(
                            UUID.randomUUID(),
                            "Pizza Calabresa",
                            "Pizza com calabresa",
                            new BigDecimal("39.90"),
                            null,
                            false,
                            List.of())))));

    when(merchantQueryPort.getPublicStoreMenu("loja-do-bairro")).thenReturn(output);

    var response = getPublicStoreMenuUseCase.execute(" loja-do-bairro ");

    assertThat(response.categories()).hasSize(2);
    assertThat(response.categories().getFirst().name()).isEqualTo("Bebidas");
    assertThat(response.categories().getLast().name()).isEqualTo("Pizzas");
  }

  @Test
  void shouldReturnNotFoundWhenSlugDoesNotExist() {
    when(merchantQueryPort.getPublicStoreMenu("nao-existe"))
        .thenThrow(new StoreSlugNotFoundException("nao-existe"));

    assertThatThrownBy(() -> getPublicStoreMenuUseCase.execute("nao-existe"))
        .isInstanceOf(StoreSlugNotFoundException.class)
        .hasMessageContaining("nao-existe");
  }
}
