package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.domain.WebhookProcessingStatus;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class PaymentWebhookReceiverServiceTest {

  private final PaymentWebhookEventRepository repository =
      mock(PaymentWebhookEventRepository.class);
  private final WebhookIdempotencyKeyResolver resolver = mock(WebhookIdempotencyKeyResolver.class);
  private final PaymentWebhookProcessor processor = mock(PaymentWebhookProcessor.class);
  private final PaymentWebhookReceiverService service =
      new PaymentWebhookReceiverService(repository, resolver, processor);

  @Test
  void shouldPersistAndProcessWebhookUsingDefaultSignatureFlag() {
    var request =
        new PaymentWebhookRequest(
            "evt-1", "PAYMENT_RECEIVED", "ref-1", "2026-03-22T12:00:00Z", null);
    var resolvedKey =
        new WebhookIdempotencyKeyResolver.ResolvedIdempotencyKey("EXTERNAL_EVENT_ID", "evt-1");
    when(resolver.resolve(request)).thenReturn(resolvedKey);
    when(repository.findByProviderNameAndIdempotencyKey("mock-psp", "evt-1"))
        .thenReturn(Optional.empty());
    when(repository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, PaymentWebhookEvent.class));

    var response = service.receive(" mock-psp ", request, "{\"x\":1}");

    var captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
    verify(repository).saveAndFlush(captor.capture());
    verify(processor).process(any(PaymentWebhookEvent.class), any(PaymentWebhookRequest.class));
    assertThat(captor.getValue().getSignatureValid()).isTrue();
    assertThat(response.processingStatus()).isEqualTo(WebhookProcessingStatus.PROCESSED);
  }

  @Test
  void shouldReturnIgnoredForDuplicateWebhookWithEquivalentNonJsonPayload() {
    var request = new PaymentWebhookRequest(null, "PAYMENT_RECEIVED", "ref-1", null, null);
    var resolvedKey =
        new WebhookIdempotencyKeyResolver.ResolvedIdempotencyKey(
            "PROVIDER_REFERENCE_EVENT_TYPE", "ref-1::payment_received");
    var existing =
        PaymentWebhookEvent.received(
            null, "mock-psp", null, "ref-1::payment_received", " not-json ");

    when(resolver.resolve(request)).thenReturn(resolvedKey);
    when(repository.findByProviderNameAndIdempotencyKey("mock-psp", "ref-1::payment_received"))
        .thenReturn(Optional.of(existing));

    var response = service.receive("mock-psp", request, "not-json");

    assertThat(response.processingStatus()).isEqualTo(WebhookProcessingStatus.IGNORED);
    assertThat(response.externalEventId()).isNull();
  }

  @Test
  void shouldRejectDuplicateWebhookWithDifferentPayload() {
    var request = new PaymentWebhookRequest(null, "PAYMENT_RECEIVED", "ref-1", null, null);
    var resolvedKey =
        new WebhookIdempotencyKeyResolver.ResolvedIdempotencyKey(
            "PROVIDER_REFERENCE_EVENT_TYPE", "ref-1::payment_received");
    var existing =
        PaymentWebhookEvent.received(
            null, "mock-psp", null, "ref-1::payment_received", "{\"amount\":10}");

    when(resolver.resolve(request)).thenReturn(resolvedKey);
    when(repository.findByProviderNameAndIdempotencyKey("mock-psp", "ref-1::payment_received"))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.receive("mock-psp", request, "{\"amount\":15}", false))
        .isInstanceOf(IdempotencyConflictException.class)
        .hasMessage("The same idempotency key was reused with a different payload.");
  }

  @Test
  void shouldReturnIgnoredWhenConcurrentInsertFindsSameEvent() {
    var request = new PaymentWebhookRequest("evt-1", "PAYMENT_RECEIVED", "ref-1", null, null);
    var resolvedKey =
        new WebhookIdempotencyKeyResolver.ResolvedIdempotencyKey("EXTERNAL_EVENT_ID", "evt-1");
    var existing = PaymentWebhookEvent.received(null, "mock-psp", "evt-1", "evt-1", "{\"x\":1}");

    when(resolver.resolve(request)).thenReturn(resolvedKey);
    when(repository.findByProviderNameAndIdempotencyKey("mock-psp", "evt-1"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existing));
    when(repository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    var response = service.receive("mock-psp", request, "{\"x\":1}", true);

    assertThat(response.processingStatus()).isEqualTo(WebhookProcessingStatus.IGNORED);
    assertThat(response.externalEventId()).isEqualTo("evt-1");
  }

  @Test
  void shouldMarkWebhookAsFailedWhenProcessorThrows() {
    var request = new PaymentWebhookRequest("evt-1", "PAYMENT_RECEIVED", "ref-1", null, null);
    var resolvedKey =
        new WebhookIdempotencyKeyResolver.ResolvedIdempotencyKey("EXTERNAL_EVENT_ID", "evt-1");
    when(resolver.resolve(request)).thenReturn(resolvedKey);
    when(repository.findByProviderNameAndIdempotencyKey("mock-psp", "evt-1"))
        .thenReturn(Optional.empty());
    when(repository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, PaymentWebhookEvent.class));
    doThrow(new IllegalStateException("boom")).when(processor).process(any(), any());

    assertThatThrownBy(() -> service.receive("mock-psp", request, "{\"x\":1}", false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");

    var captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getProcessingStatus()).isEqualTo(WebhookProcessingStatus.FAILED);
  }

  @Test
  void shouldRejectBlankProvider() {
    var request = new PaymentWebhookRequest("evt-1", "PAYMENT_RECEIVED", "ref-1", null, null);

    assertThatThrownBy(() -> service.receive(" ", request, "{\"x\":1}", true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("provider must not be blank");
  }

  @Test
  void shouldRethrowIntegrityViolationWhenConcurrentLookupDoesNotFindEvent() {
    var request = new PaymentWebhookRequest("evt-1", "PAYMENT_RECEIVED", "ref-1", null, null);
    var resolvedKey =
        new WebhookIdempotencyKeyResolver.ResolvedIdempotencyKey("EXTERNAL_EVENT_ID", "evt-1");

    when(resolver.resolve(request)).thenReturn(resolvedKey);
    when(repository.findByProviderNameAndIdempotencyKey("mock-psp", "evt-1"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.empty());
    when(repository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    assertThatThrownBy(() -> service.receive("mock-psp", request, "{\"x\":1}", true))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessage("duplicate");
  }

  @Test
  void shouldRejectNullProviderAndTrimNullableNull() throws Exception {
    var request = new PaymentWebhookRequest("evt-1", "PAYMENT_RECEIVED", "ref-1", null, null);

    assertThatThrownBy(() -> service.receive(null, request, "{\"x\":1}", true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("provider must not be blank");

    Method method =
        PaymentWebhookReceiverService.class.getDeclaredMethod("trimNullable", String.class);
    method.setAccessible(true);
    assertThat(method.invoke(service, new Object[] {null})).isNull();
    assertThat(method.invoke(service, " payload ")).isEqualTo("payload");
  }
}
