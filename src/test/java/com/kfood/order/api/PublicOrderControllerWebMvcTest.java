package com.kfood.order.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.order.app.CreatePublicOrderOutput;
import com.kfood.order.app.CreatePublicOrderService;
import com.kfood.order.app.GetPublicOrderByNumberUseCase;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.app.PublicOrderLookupOutput;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
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
class PublicOrderControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreatePublicOrderService createPublicOrderService;
  @MockitoBean private GetPublicOrderByNumberUseCase getPublicOrderByNumberUseCase;

  @Test
  void shouldReturn201WhenOrderIsCreated() throws Exception {
    var orderId = UUID.randomUUID();
    var quoteId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    when(createPublicOrderService.create(eq("loja-do-bairro"), eq("idem-123"), any()))
        .thenReturn(
            new CreatePublicOrderOutput(
                orderId,
                "PED-20260321-000001",
                OrderStatus.NEW,
                PaymentStatusSnapshot.PENDING,
                new BigDecimal("50.00"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("50.00"),
                Instant.now()));

    mockMvc
        .perform(
            post("/v1/public/stores/loja-do-bairro/orders")
                .header("Idempotency-Key", "idem-123")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "quoteId": "%s",
                      "customerId": "%s",
                      "fulfillmentType": "%s",
                      "paymentMethod": "%s",
                      "notes": "Sem cebola"
                    }
                    """
                        .formatted(quoteId, customerId, FulfillmentType.PICKUP, PaymentMethod.PIX)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderNumber").value("PED-20260321-000001"))
        .andExpect(jsonPath("$.status").value("NEW"))
        .andExpect(jsonPath("$.paymentStatusSnapshot").value("PENDING"));
  }

  @Test
  void shouldReturn200WhenPublicOrderIsFound() throws Exception {
    when(getPublicOrderByNumberUseCase.execute("loja-do-bairro", "PED-20260326-000123"))
        .thenReturn(
            new PublicOrderLookupOutput(
                "PED-20260326-000123",
                OrderStatus.PREPARING,
                PaymentStatusSnapshot.PENDING,
                FulfillmentType.DELIVERY,
                new BigDecimal("50.00"),
                new BigDecimal("6.50"),
                new BigDecimal("56.50"),
                Instant.parse("2026-03-26T15:00:00Z"),
                null));

    mockMvc
        .perform(get("/v1/public/stores/loja-do-bairro/orders/PED-20260326-000123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderNumber").value("PED-20260326-000123"))
        .andExpect(jsonPath("$.status").value("PREPARING"))
        .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
        .andExpect(jsonPath("$.paymentStatusSnapshot").value("PENDING"))
        .andExpect(jsonPath("$.fulfillmentType").value("DELIVERY"))
        .andExpect(jsonPath("$.subtotalAmount").value(50.00))
        .andExpect(jsonPath("$.deliveryFeeAmount").value(6.50))
        .andExpect(jsonPath("$.totalAmount").value(56.50))
        .andExpect(jsonPath("$.createdAt").value("2026-03-26T15:00:00Z"))
        .andExpect(jsonPath("$.scheduledFor").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.customerId").doesNotExist())
        .andExpect(jsonPath("$.notes").doesNotExist())
        .andExpect(jsonPath("$.paymentMethod").doesNotExist());
  }

  @Test
  void shouldReturn404WhenStoreDoesNotExist() throws Exception {
    when(getPublicOrderByNumberUseCase.execute("nao-existe", "PED-20260326-000123"))
        .thenThrow(new StoreSlugNotFoundException("nao-existe"));

    mockMvc
        .perform(get("/v1/public/stores/nao-existe/orders/PED-20260326-000123"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldReturn404WhenOrderDoesNotExist() throws Exception {
    when(getPublicOrderByNumberUseCase.execute("loja-do-bairro", "PED-20260326-000999"))
        .thenThrow(new OrderNotFoundException("PED-20260326-000999"));

    mockMvc
        .perform(get("/v1/public/stores/loja-do-bairro/orders/PED-20260326-000999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldReturn404WhenOrderDoesNotBelongToStore() throws Exception {
    when(getPublicOrderByNumberUseCase.execute("loja-do-bairro", "PED-20260326-000555"))
        .thenThrow(new OrderNotFoundException("PED-20260326-000555"));

    mockMvc
        .perform(get("/v1/public/stores/loja-do-bairro/orders/PED-20260326-000555"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }
}
