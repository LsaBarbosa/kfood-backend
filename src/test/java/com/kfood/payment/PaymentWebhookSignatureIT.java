package com.kfood.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "app.payment.webhook-security.providers.mock-psp.mode=HMAC_SHA256",
      "app.payment.webhook-security.providers.mock-psp.required=true",
      "app.payment.webhook-security.providers.mock-psp.signature-header=X-Signature",
      "app.payment.webhook-security.providers.mock-psp.hmac-secret=test-secret"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentWebhookSignatureIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private PaymentWebhookEventRepository repository;

  @AfterEach
  void tearDown() {
    repository.deleteAll();
  }

  @Test
  void shouldAllowProcessingWhenSignatureIsValid() throws Exception {
    var payload =
        """
        {
          "externalEventId": "evt_sig_valid_001",
          "eventType": "PAYMENT_RECEIVED",
          "providerReference": "psp_ref_sig_001",
          "amount": 56.50
        }
        """;

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp")
                .header("X-Signature", "sha256=" + hmacHex("test-secret", payload))
                .contentType(APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.accepted").value(true))
        .andExpect(jsonPath("$.processingStatus").value("PROCESSED"));

    var events = repository.findAll();
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getSignatureValid()).isTrue();
    assertThat(events.getFirst().getProcessingStatus().name()).isEqualTo("PROCESSED");
  }

  @Test
  void shouldRejectRequestWhenSignatureIsInvalid() throws Exception {
    var payload =
        """
        {
          "externalEventId": "evt_sig_invalid_001",
          "eventType": "PAYMENT_RECEIVED",
          "providerReference": "psp_ref_sig_002",
          "amount": 56.50
        }
        """;

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp")
                .header("X-Signature", "sha256=invalid-signature")
                .contentType(APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("WEBHOOK_SIGNATURE_INVALID"))
        .andExpect(jsonPath("$.message").value("Webhook signature or token is invalid."));

    var events = repository.findAll();
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getSignatureValid()).isFalse();
    assertThat(events.getFirst().getProcessingStatus().name()).isEqualTo("FAILED");
  }

  @Test
  void shouldRejectWhenSignatureIsMissingAndRequired() throws Exception {
    var payload =
        """
        {
          "externalEventId": "evt_sig_missing_001",
          "eventType": "PAYMENT_RECEIVED",
          "providerReference": "psp_ref_sig_003",
          "amount": 56.50
        }
        """;

    mockMvc
        .perform(
            post("/v1/payments/webhooks/mock-psp").contentType(APPLICATION_JSON).content(payload))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("WEBHOOK_SIGNATURE_INVALID"))
        .andExpect(jsonPath("$.message").value("Webhook signature or token is invalid."));

    var events = repository.findAll();
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getSignatureValid()).isFalse();
    assertThat(events.getFirst().getProcessingStatus().name()).isEqualTo("FAILED");
  }

  private String hmacHex(String secret, String payload) throws Exception {
    var mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
  }
}
