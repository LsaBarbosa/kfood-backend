package com.kfood.merchant.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.merchant.app.GetPublicStoreMenuUseCase;
import com.kfood.merchant.app.GetPublicStoreUseCase;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.domain.StoreStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "app.security.jwt-secret=12345678901234567890123456789012",
      "app.security.jwt-expiration-seconds=3600"
    })
class PublicStoreControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetPublicStoreUseCase getPublicStoreUseCase;

  @MockitoBean private GetPublicStoreMenuUseCase getPublicStoreMenuUseCase;

  @Test
  void shouldReturnPublicStoreWithoutAuthentication() throws Exception {
    when(getPublicStoreUseCase.execute("loja-do-bairro"))
        .thenReturn(
            new PublicStoreResponse(
                "loja-do-bairro",
                "Loja do Bairro",
                StoreStatus.ACTIVE,
                "21999990000",
                List.of(
                    new PublicStoreHourResponse(
                        DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false)),
                List.of(
                    new PublicDeliveryZoneResponse(
                        "Centro", new BigDecimal("6.50"), new BigDecimal("25.00")))));

    mockMvc
        .perform(get("/v1/public/stores/loja-do-bairro"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("loja-do-bairro"))
        .andExpect(jsonPath("$.name").value("Loja do Bairro"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.phone").value("21999990000"))
        .andExpect(jsonPath("$.hours[0].dayOfWeek").value("MONDAY"))
        .andExpect(jsonPath("$.hours[0].closed").value(false))
        .andExpect(jsonPath("$.deliveryZones[0].zoneName").value("Centro"))
        .andExpect(jsonPath("$.deliveryZones[0].feeAmount").value(6.5))
        .andExpect(jsonPath("$.deliveryZones[0].minOrderAmount").value(25.0))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.cnpj").doesNotExist())
        .andExpect(jsonPath("$.timezone").doesNotExist());
  }

  @Test
  void shouldReturnNotFoundWhenStoreSlugDoesNotExist() throws Exception {
    when(getPublicStoreUseCase.execute("nao-existe"))
        .thenThrow(new StoreSlugNotFoundException("nao-existe"));

    mockMvc
        .perform(get("/v1/public/stores/nao-existe"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldReturnPublicMenuWithoutAuthentication() throws Exception {
    when(getPublicStoreMenuUseCase.execute("loja-do-bairro"))
        .thenReturn(
            new PublicStoreMenuResponse(
                List.of(
                    new PublicStoreMenuCategoryResponse(
                        java.util.UUID.randomUUID(),
                        "Bebidas",
                        List.of(
                            new PublicStoreMenuProductResponse(
                                java.util.UUID.randomUUID(),
                                "Refrigerante",
                                "Lata 350ml",
                                new BigDecimal("7.50"),
                                null,
                                false))))));

    mockMvc
        .perform(get("/v1/public/stores/loja-do-bairro/menu"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[0].name").value("Bebidas"))
        .andExpect(jsonPath("$.categories[0].products[0].name").value("Refrigerante"))
        .andExpect(jsonPath("$.categories[0].products[0].basePrice").value(7.5))
        .andExpect(jsonPath("$.categories[0].products[0].paused").value(false));
  }

  @Test
  void shouldReturnNotFoundWhenStoreMenuSlugDoesNotExist() throws Exception {
    when(getPublicStoreMenuUseCase.execute("nao-existe"))
        .thenThrow(new StoreSlugNotFoundException("nao-existe"));

    mockMvc
        .perform(get("/v1/public/stores/nao-existe/menu"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }
}
