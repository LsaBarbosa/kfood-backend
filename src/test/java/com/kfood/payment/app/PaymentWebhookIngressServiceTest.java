package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.api.PaymentWebhookResponse;
import com.kfood.payment.domain.WebhookProcessingStatus;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class PaymentWebhookIngressServiceTest {

  private final PaymentWebhookAuthenticator authenticator = mock(PaymentWebhookAuthenticator.class);
  private final RejectedWebhookRecorder rejectedWebhookRecorder =
      mock(RejectedWebhookRecorder.class);
  private final PaymentWebhookReceiverService receiverService =
      mock(PaymentWebhookReceiverService.class);
  private final PaymentWebhookIngressService service =
      new PaymentWebhookIngressService(
          authenticator,
          rejectedWebhookRecorder,
          receiverService,
          Validation.buildDefaultValidatorFactory().getValidator());

  @Test
  void shouldRejectBlankPayload() {
    assertThatThrownBy(() -> service.receive("mock-psp", new HttpHeaders(), " "))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode", "status", "message")
        .containsExactly(
            ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Invalid webhook payload.");
  }

  @Test
  void shouldParseNonTextValuesAndStringAmount() {
    when(receiverService.receive(eq("mock-psp"), any(PaymentWebhookRequest.class), any(), eq(true)))
        .thenReturn(new PaymentWebhookResponse(true, WebhookProcessingStatus.PROCESSED, "123"));

    var payload =
        """
        {
          "externalEventId": 123,
          "eventType": "PAYMENT_RECEIVED",
          "providerReference": 456,
          "paidAt": 789,
          "amount": "10.50"
        }
        """;

    var response = service.receive("mock-psp", new HttpHeaders(), payload);

    var captor = ArgumentCaptor.forClass(PaymentWebhookRequest.class);
    verify(receiverService).receive(eq("mock-psp"), captor.capture(), eq(payload), eq(true));
    assertThat(captor.getValue().externalEventId()).isEqualTo("123");
    assertThat(captor.getValue().providerReference()).isEqualTo("456");
    assertThat(captor.getValue().paidAt()).isEqualTo("789");
    assertThat(captor.getValue().amount()).isEqualByComparingTo("10.50");
    assertThat(response.processingStatus()).isEqualTo(WebhookProcessingStatus.PROCESSED);
  }

  @Test
  void shouldAllowMissingOptionalAmount() {
    when(receiverService.receive(eq("mock-psp"), any(PaymentWebhookRequest.class), any(), eq(true)))
        .thenReturn(new PaymentWebhookResponse(true, WebhookProcessingStatus.PROCESSED, "evt-1"));

    var payload =
        """
        {
          "externalEventId": "evt-1",
          "eventType": "PAYMENT_RECEIVED",
          "providerReference": "ref-1"
        }
        """;

    service.receive("mock-psp", new HttpHeaders(), payload);

    var captor = ArgumentCaptor.forClass(PaymentWebhookRequest.class);
    verify(receiverService).receive(eq("mock-psp"), captor.capture(), eq(payload), eq(true));
    assertThat(captor.getValue().amount()).isNull();
  }

  @Test
  void shouldRecordRejectedWebhookWhenSignatureIsInvalid() {
    var headers = new HttpHeaders();
    var payload = "{\"externalEventId\":\"evt-1\"}";
    doThrow(new WebhookSignatureInvalidException("Webhook signature or token is invalid."))
        .when(authenticator)
        .authenticate("mock-psp", payload, headers);

    assertThatThrownBy(() -> service.receive("mock-psp", headers, payload))
        .isInstanceOf(WebhookSignatureInvalidException.class);

    verify(rejectedWebhookRecorder).recordInvalidSignature("mock-psp", payload);
  }
}
