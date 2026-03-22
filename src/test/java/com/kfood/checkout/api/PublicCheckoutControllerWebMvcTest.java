package com.kfood.checkout.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.checkout.app.CalculateCheckoutQuoteUseCase;
import java.util.List;
import java.util.UUID;
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
class PublicCheckoutControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CalculateCheckoutQuoteUseCase calculateCheckoutQuoteUseCase;

  @Test
  void shouldCalculateQuoteWithoutAuthentication() throws Exception {
    var storeId = UUID.randomUUID();
    var quoteId = UUID.randomUUID();
    when(calculateCheckoutQuoteUseCase.execute(eq("loja-do-bairro"), any()))
        .thenReturn(
            new QuoteCheckoutResponse(
                quoteId,
                storeId,
                new java.math.BigDecimal("42.00"),
                new java.math.BigDecimal("6.50"),
                new java.math.BigDecimal("48.50"),
                35,
                List.of()));

    mockMvc
        .perform(
            post("/v1/public/stores/loja-do-bairro/checkout/quote")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "customerId": "11111111-1111-1111-1111-111111111111",
                      "fulfillmentType": "DELIVERY",
                      "addressId": "22222222-2222-2222-2222-222222222222",
                      "items": [
                        {
                          "productId": "33333333-3333-3333-3333-333333333333",
                          "quantity": 1
                        }
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quoteId").value(quoteId.toString()))
        .andExpect(jsonPath("$.storeId").value(storeId.toString()))
        .andExpect(jsonPath("$.totalAmount").value(48.50));
  }

  @Test
  void shouldReturnBadRequestWhenItemsAreMissing() throws Exception {
    mockMvc
        .perform(
            post("/v1/public/stores/loja-do-bairro/checkout/quote")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "customerId": "11111111-1111-1111-1111-111111111111",
                      "fulfillmentType": "PICKUP",
                      "items": []
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
