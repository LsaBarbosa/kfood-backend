package com.kfood.order.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.order.app.CreatePublicOrderService;
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

  @Test
  void shouldReturn201WhenOrderIsCreated() throws Exception {
    var orderId = UUID.randomUUID();
    var quoteId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    when(createPublicOrderService.create(eq("loja-do-bairro"), eq("idem-123"), any()))
        .thenReturn(
            new CreatePublicOrderResponse(
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
}
