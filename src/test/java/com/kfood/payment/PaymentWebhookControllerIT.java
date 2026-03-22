package com.kfood.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentWebhookControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private PaymentWebhookEventRepository paymentWebhookEventRepository;

  @AfterEach
  void tearDown() {
    paymentWebhookEventRepository.deleteAll();
  }

  @Test
  void shouldAcceptValidWebhook() throws Exception {
    var payload =
        """
        {
          "externalEventId": "evt_123",
          "eventType": "PAYMENT_RECEIVED",
          "providerReference": "psp_123456",
          "paidAt": "2026-03-16T18:50:00Z",
          "amount": 56.50
        }
        """;

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp").contentType(APPLICATION_JSON).content(payload))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.accepted").value(true))
        .andExpect(jsonPath("$.processingStatus").value("PROCESSED"))
        .andExpect(jsonPath("$.externalEventId").value("evt_123"));

    var events = paymentWebhookEventRepository.findAll();

    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getProviderName()).isEqualTo("mock-psp");
    assertThat(events.getFirst().getExternalEventId()).isEqualTo("evt_123");
    assertThat(events.getFirst().getProcessingStatus().name()).isEqualTo("PROCESSED");
    assertThat(events.getFirst().getRawPayload()).contains("\"eventType\": \"PAYMENT_RECEIVED\"");
  }

  @Test
  void shouldRejectInvalidJsonPayload() throws Exception {
    var payload =
        """
        {
          "externalEventId": "evt_123",
          "eventType":
        }
        """;

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp").contentType(APPLICATION_JSON).content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Invalid webhook payload."));

    assertThat(paymentWebhookEventRepository.findAll()).isEmpty();
  }

  @Test
  void shouldRejectWebhookWithoutRequiredFields() throws Exception {
    var payload =
        """
        {
          "eventType": "PAYMENT_CONFIRMED",
          "amount": 56.50
        }
        """;

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp").contentType(APPLICATION_JSON).content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.message").value("Webhook payload has missing or invalid required fields."));

    assertThat(paymentWebhookEventRepository.findAll()).isEmpty();
  }
}
