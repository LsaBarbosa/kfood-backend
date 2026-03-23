package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RejectedWebhookRecorderTest {

  @Test
  void shouldRecordInvalidSignatureWebhook() {
    var repository = mock(PaymentWebhookEventRepository.class);
    when(repository.save(any(PaymentWebhookEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, PaymentWebhookEvent.class));
    var recorder = new RejectedWebhookRecorder(repository);

    recorder.recordInvalidSignature(
        " MOCK-PSP ",
        """
        {"externalEventId":"evt-1","eventType":"PAYMENT_RECEIVED"}
        """);

    var captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getProviderName()).isEqualTo("mock-psp");
    assertThat(captor.getValue().getExternalEventId()).isEqualTo("evt-1");
    assertThat(captor.getValue().getSignatureValid()).isFalse();
    assertThat(captor.getValue().getProcessingStatus().name()).isEqualTo("FAILED");
    assertThat(captor.getValue().getIdempotencyKey()).startsWith("auth-failed::");
  }

  @Test
  void shouldRecordInvalidSignatureWebhookWithoutExternalEventId() {
    var repository = mock(PaymentWebhookEventRepository.class);
    when(repository.save(any(PaymentWebhookEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, PaymentWebhookEvent.class));
    var recorder = new RejectedWebhookRecorder(repository);

    recorder.recordInvalidSignature("mock-psp", "not-json");

    var captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getExternalEventId()).isNull();
    assertThat(captor.getValue().getRawPayload()).isEqualTo("not-json");
  }

  @Test
  void shouldUseEmptyJsonWhenPayloadIsBlank() {
    var repository = mock(PaymentWebhookEventRepository.class);
    when(repository.save(any(PaymentWebhookEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, PaymentWebhookEvent.class));
    var recorder = new RejectedWebhookRecorder(repository);

    recorder.recordInvalidSignature("mock-psp", " ");

    var captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getRawPayload()).isEqualTo("{}");
    assertThat(captor.getValue().getExternalEventId()).isNull();
  }

  @Test
  void shouldUseEmptyJsonWhenPayloadIsNull() {
    var repository = mock(PaymentWebhookEventRepository.class);
    when(repository.save(any(PaymentWebhookEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, PaymentWebhookEvent.class));
    var recorder = new RejectedWebhookRecorder(repository);

    recorder.recordInvalidSignature("mock-psp", null);

    var captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getRawPayload()).isEqualTo("{}");
  }

  @Test
  void shouldTreatExplicitNullExternalEventIdAsMissing() {
    var repository = mock(PaymentWebhookEventRepository.class);
    when(repository.save(any(PaymentWebhookEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, PaymentWebhookEvent.class));
    var recorder = new RejectedWebhookRecorder(repository);

    recorder.recordInvalidSignature("mock-psp", "{\"externalEventId\":null}");

    var captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getExternalEventId()).isNull();
  }
}
