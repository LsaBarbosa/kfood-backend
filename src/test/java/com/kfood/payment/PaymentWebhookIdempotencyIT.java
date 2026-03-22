package com.kfood.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.app.PaymentWebhookProcessor;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PaymentWebhookIdempotencyIT.TestConfig.class)
class PaymentWebhookIdempotencyIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private PaymentWebhookEventRepository repository;

  @Autowired private CountingPaymentWebhookProcessor paymentWebhookProcessor;

  @AfterEach
  void tearDown() {
    repository.deleteAll();
    paymentWebhookProcessor.reset();
  }

  @Test
  void shouldProcessSameEventOnlyOnce() throws Exception {
    var payload =
        """
        {
          "externalEventId": "evt_9001",
          "eventType": "PAYMENT_CONFIRMED",
          "providerReference": "psp_ref_001",
          "amount": 57.90
        }
        """;

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp").contentType(APPLICATION_JSON).content(payload))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.accepted").value(true))
        .andExpect(jsonPath("$.processingStatus").value("PROCESSED"));

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp").contentType(APPLICATION_JSON).content(payload))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.accepted").value(true))
        .andExpect(jsonPath("$.processingStatus").value("IGNORED"));

    assertThat(paymentWebhookProcessor.invocationCount()).isEqualTo(1);
    assertThat(repository.findAll()).hasSize(1);
    assertThat(repository.findAll().getFirst().getProcessingStatus().name()).isEqualTo("PROCESSED");
  }

  @Test
  void shouldUseFallbackWhenExternalEventIdIsMissing() throws Exception {
    var payload =
        """
        {
          "eventType": "PAYMENT_CONFIRMED",
          "providerReference": "psp_ref_fallback_001",
          "amount": 10.00
        }
        """;

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp").contentType(APPLICATION_JSON).content(payload))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.processingStatus").value("PROCESSED"));

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp").contentType(APPLICATION_JSON).content(payload))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.processingStatus").value("IGNORED"));

    assertThat(paymentWebhookProcessor.invocationCount()).isEqualTo(1);
    assertThat(repository.findAll()).hasSize(1);
    assertThat(repository.findAll().getFirst().getIdempotencyKey())
        .isEqualTo("psp_ref_fallback_001::payment_confirmed");
  }

  @Test
  void shouldReturnConflictWhenSameIdempotencyKeyIsReusedWithDifferentPayload() throws Exception {
    var firstPayload =
        """
        {
          "eventType": "PAYMENT_CONFIRMED",
          "providerReference": "psp_conflict_001",
          "amount": 10.00
        }
        """;
    var secondPayload =
        """
        {
          "eventType": "PAYMENT_CONFIRMED",
          "providerReference": "psp_conflict_001",
          "amount": 15.00
        }
        """;

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp")
                .contentType(APPLICATION_JSON)
                .content(firstPayload))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.processingStatus").value("PROCESSED"));

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp")
                .contentType(APPLICATION_JSON)
                .content(secondPayload))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"));

    assertThat(paymentWebhookProcessor.invocationCount()).isEqualTo(1);
    assertThat(repository.findAll()).hasSize(1);
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    CountingPaymentWebhookProcessor countingPaymentWebhookProcessor() {
      return new CountingPaymentWebhookProcessor();
    }
  }

  static class CountingPaymentWebhookProcessor implements PaymentWebhookProcessor {

    private final AtomicInteger invocationCount = new AtomicInteger();

    @Override
    public void process(PaymentWebhookEvent event, PaymentWebhookRequest request) {
      invocationCount.incrementAndGet();
    }

    int invocationCount() {
      return invocationCount.get();
    }

    void reset() {
      invocationCount.set(0);
    }
  }
}
