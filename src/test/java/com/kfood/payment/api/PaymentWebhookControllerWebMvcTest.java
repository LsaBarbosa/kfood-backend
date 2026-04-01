package com.kfood.payment.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.identity.app.JwtTokenReader;
import com.kfood.payment.app.MockPaymentWebhookAuthenticator;
import com.kfood.payment.app.PaymentWebhookAuthenticationService;
import com.kfood.payment.app.PaymentWebhookRegisteredEvent;
import com.kfood.payment.app.PaymentWebhookRegisteredPublisher;
import com.kfood.payment.app.RegisterPaymentWebhookUseCase;
import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.shared.config.AppProperties;
import com.kfood.shared.exceptions.ApiErrorResponseFactory;
import com.kfood.shared.exceptions.GlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
  PaymentWebhookAuthenticationService.class,
  MockPaymentWebhookAuthenticator.class,
  RegisterPaymentWebhookUseCase.class,
  GlobalExceptionHandler.class,
  ApiErrorResponseFactory.class,
  PaymentWebhookControllerWebMvcTest.TestConfig.class
})
class PaymentWebhookControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort;
  @MockitoBean private PaymentWebhookRegisteredPublisher paymentWebhookRegisteredPublisher;
  @MockitoBean private JwtTokenReader jwtTokenReader;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private AccessDeniedHandler accessDeniedHandler;

  @Test
  @DisplayName("should accept valid webhook payload")
  void shouldAcceptValidWebhookPayload() throws Exception {
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    var savedEvent = savedEvent();
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), any(), any(), any(), anyBoolean(), any(), any()))
        .thenReturn(savedEvent);

    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .header("X-Webhook-Token", "mock-token")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalEventId": "evt-123",
                      "eventType": "PAYMENT_CONFIRMED",
                      "providerReference": "charge-123"
                    }
                    """))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.accepted").value(true))
        .andExpect(jsonPath("$.processingStatus").value("RECEIVED"))
        .andExpect(jsonPath("$.externalEventId").value("evt-123"));

    verify(paymentWebhookEventPersistencePort)
        .saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(true), any(), any());
    verify(paymentWebhookRegisteredPublisher)
        .publish(
            new PaymentWebhookRegisteredEvent(
                savedEvent.getId(), "charge-123", "PAYMENT_CONFIRMED"));
  }

  @Test
  @DisplayName("should return accepted for replayed webhook payload")
  void shouldReturnAcceptedForReplayedWebhookPayload() throws Exception {
    var existingEvent = savedEvent();
    existingEvent.markProcessed(Instant.parse("2026-03-30T15:01:00Z"));
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.of(existingEvent));

    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .header("X-Webhook-Token", "mock-token")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalEventId": "evt-123",
                      "eventType": "PAYMENT_CONFIRMED"
                    }
                    """))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.accepted").value(true))
        .andExpect(jsonPath("$.processingStatus").value("PROCESSED"))
        .andExpect(jsonPath("$.externalEventId").value("evt-123"));

    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("should return unauthorized when webhook token is missing")
  void shouldReturnUnauthorizedWhenWebhookTokenIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalEventId": "evt-123",
                      "eventType": "PAYMENT_CONFIRMED"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("WEBHOOK_SIGNATURE_INVALID"));

    verify(paymentWebhookEventPersistencePort, never())
        .findByProviderNameAndExternalEventId(any(), any());
    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("should return unauthorized when webhook token is blank")
  void shouldReturnUnauthorizedWhenWebhookTokenIsBlank() throws Exception {
    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .header("X-Webhook-Token", "   ")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalEventId": "evt-123",
                      "eventType": "PAYMENT_CONFIRMED"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("WEBHOOK_SIGNATURE_INVALID"));

    verify(paymentWebhookEventPersistencePort, never())
        .findByProviderNameAndExternalEventId(any(), any());
    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("should return unauthorized when webhook token is invalid")
  void shouldReturnUnauthorizedWhenWebhookTokenIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .header("X-Webhook-Token", "wrong-token")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalEventId": "evt-123",
                      "eventType": "PAYMENT_CONFIRMED"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("WEBHOOK_SIGNATURE_INVALID"));

    verify(paymentWebhookEventPersistencePort, never())
        .findByProviderNameAndExternalEventId(any(), any());
    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("should return bad request when json is malformed")
  void shouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .header("X-Webhook-Token", "mock-token")
                .contentType(APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("body"))
        .andExpect(jsonPath("$.details[0].message").value("Malformed JSON payload."));

    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  @DisplayName("should return bad request when external event id is missing")
  void shouldReturnBadRequestWhenExternalEventIdIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .header("X-Webhook-Token", "mock-token")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "eventType": "PAYMENT_CONFIRMED"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("externalEventId"))
        .andExpect(jsonPath("$.details[0].message").value("externalEventId must not be blank"));
  }

  @Test
  @DisplayName("should return bad request when event type is missing")
  void shouldReturnBadRequestWhenEventTypeIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .header("X-Webhook-Token", "mock-token")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalEventId": "evt-123"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("eventType"))
        .andExpect(jsonPath("$.details[0].message").value("eventType must not be blank"));
  }

  @Test
  @DisplayName("should return bad request when external event id is blank")
  void shouldReturnBadRequestWhenExternalEventIdIsBlank() throws Exception {
    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .header("X-Webhook-Token", "mock-token")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalEventId": "   ",
                      "eventType": "PAYMENT_CONFIRMED"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("externalEventId"))
        .andExpect(jsonPath("$.details[0].message").value("externalEventId must not be blank"));
  }

  @Test
  @DisplayName(
      "should accept confirmed webhook with missing provider reference without confirming payment")
  void shouldAcceptConfirmedWebhookWithMissingProviderReferenceWithoutConfirmingPayment()
      throws Exception {
    var receivedEvent = savedEvent();
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-999"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), any(), any(), any(), anyBoolean(), any(), any()))
        .thenReturn(receivedEvent);

    mockMvc
        .perform(
            post("/v1/payments/webhooks/{provider}", "mock")
                .header("X-Webhook-Token", "mock-token")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalEventId": "evt-999",
                      "eventType": "PAYMENT_CONFIRMED"
                    }
                    """))
        .andExpect(status().isAccepted());

    verify(paymentWebhookRegisteredPublisher)
        .publish(
            new PaymentWebhookRegisteredEvent(receivedEvent.getId(), null, "PAYMENT_CONFIRMED"));
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    AppProperties appProperties() {
      var properties = new AppProperties();
      properties.getPayment().getWebhook().getProviders().getMock().setToken("mock-token");
      return properties;
    }

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-03-30T16:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }
  }

  private com.kfood.payment.infra.persistence.PaymentWebhookEvent savedEvent() {
    return new com.kfood.payment.infra.persistence.PaymentWebhookEvent(
        UUID.randomUUID(),
        null,
        "mock",
        "evt-123",
        "PAYMENT_CONFIRMED",
        false,
        "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
        Instant.parse("2026-03-30T16:00:00Z"));
  }
}
