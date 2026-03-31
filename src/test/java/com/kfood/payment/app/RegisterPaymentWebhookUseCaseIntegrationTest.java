package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.app.port.PaymentWebhookEventRecord;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import com.kfood.payment.infra.persistence.PaymentWebhookEventPersistenceAdapter;
import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@ActiveProfiles("test")
@Import({
  TestJpaAuditingConfig.class,
  PaymentWebhookEventPersistenceAdapter.class,
  RegisterPaymentWebhookUseCase.class,
  RegisterPaymentWebhookUseCaseIntegrationTest.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class RegisterPaymentWebhookUseCaseIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private RegisterPaymentWebhookUseCase useCase;
  @Autowired private PaymentWebhookEventRepository paymentWebhookEventRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private RaceInjectingPaymentWebhookEventPersistencePort raceInjectingPort;

  @MockitoBean private PaymentWebhookRegisteredPublisher paymentWebhookRegisteredPublisher;

  @Test
  @DisplayName("should keep a single persisted record across equivalent replay attempts")
  void shouldKeepSinglePersistedRecordAcrossEquivalentReplayAttempts() {
    var command =
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-replay",
              "eventType": "PAYMENT_PENDING"
            }
            """,
            true);

    var firstResult = useCase.execute(command);
    var replayResult = useCase.execute(command);

    assertThat(replayResult.getId()).isEqualTo(firstResult.getId());
    assertThat(replayResult.isSignatureValid()).isTrue();
    assertThat(replayResult.getProcessingStatus())
        .isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    assertThat(
            paymentWebhookEventRepository.findByProviderNameAndExternalEventId(
                "mock", "evt-replay"))
        .isPresent()
        .get()
        .satisfies(
            persisted -> {
              assertThat(persisted.getId()).isEqualTo(firstResult.getId());
              assertThat(persisted.getProcessingStatus())
                  .isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
              assertThat(persisted.getProcessedAt()).isNotNull();
            });
    assertThat(countRows("evt-replay")).isEqualTo(1);
  }

  @Test
  @DisplayName("should recover existing event when concurrent insert wins before adapter flush")
  void shouldRecoverExistingEventWhenConcurrentInsertWinsBeforeAdapterFlush() {
    raceInjectingPort.armNextSave("mock", "evt-race");

    var result =
        useCase.execute(
            new RegisterPaymentWebhookCommand(
                "mock",
                """
                {
                  "externalEventId": "evt-race",
                  "eventType": "PAYMENT_PENDING"
                }
                """,
                true));

    var persistedEvent =
        paymentWebhookEventRepository
            .findByProviderNameAndExternalEventId("mock", "evt-race")
            .orElseThrow();

    assertThat(result.getId()).isEqualTo(persistedEvent.getId());
    assertThat(result.isSignatureValid()).isTrue();
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    assertThat(countRows("evt-race")).isEqualTo(1);
  }

  private int countRows(String externalEventId) {
    return jdbcTemplate.queryForObject(
        """
        select count(*)
        from payment_webhook_event
        where provider_name = ? and external_event_id = ?
        """,
        Integer.class,
        "mock",
        externalEventId);
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-03-30T16:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    @Primary
    RaceInjectingPaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort(
        PaymentWebhookEventPersistenceAdapter delegate,
        PaymentWebhookEventRepository paymentWebhookEventRepository,
        PlatformTransactionManager transactionManager) {
      return new RaceInjectingPaymentWebhookEventPersistencePort(
          delegate, paymentWebhookEventRepository, transactionManager);
    }
  }

  static final class RaceInjectingPaymentWebhookEventPersistencePort
      implements PaymentWebhookEventPersistencePort {

    private final PaymentWebhookEventPersistenceAdapter delegate;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final TransactionTemplate transactionTemplate;
    private String armedProviderName;
    private String armedExternalEventId;

    RaceInjectingPaymentWebhookEventPersistencePort(
        PaymentWebhookEventPersistenceAdapter delegate,
        PaymentWebhookEventRepository paymentWebhookEventRepository,
        PlatformTransactionManager transactionManager) {
      this.delegate = delegate;
      this.paymentWebhookEventRepository = paymentWebhookEventRepository;
      this.transactionTemplate = new TransactionTemplate(transactionManager);
      this.transactionTemplate.setPropagationBehavior(
          TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    void armNextSave(String providerName, String externalEventId) {
      this.armedProviderName = providerName;
      this.armedExternalEventId = externalEventId;
    }

    @Override
    public Optional<PaymentWebhookEventRecord> findByProviderNameAndExternalEventId(
        String providerName, String externalEventId) {
      return delegate.findByProviderNameAndExternalEventId(providerName, externalEventId);
    }

    @Override
    public Optional<PaymentWebhookEventRecord> findById(UUID eventId) {
      return delegate.findById(eventId);
    }

    @Override
    public PaymentWebhookEventRecord saveReceivedEvent(
        UUID eventId,
        String providerName,
        String externalEventId,
        String eventType,
        boolean signatureValid,
        String rawPayload,
        Instant receivedAt) {
      maybeInsertConflictingRow(
          providerName, externalEventId, eventType, signatureValid, rawPayload, receivedAt);
      return delegate.saveReceivedEvent(
          eventId,
          providerName,
          externalEventId,
          eventType,
          signatureValid,
          rawPayload,
          receivedAt);
    }

    @Override
    public PaymentWebhookEventRecord markProcessed(
        UUID eventId, UUID paymentId, Instant processedAt) {
      return delegate.markProcessed(eventId, paymentId, processedAt);
    }

    @Override
    public PaymentWebhookEventRecord markFailedProcessing(UUID eventId, Instant processedAt) {
      return delegate.markFailedProcessing(eventId, processedAt);
    }

    private void maybeInsertConflictingRow(
        String providerName,
        String externalEventId,
        String eventType,
        boolean signatureValid,
        String rawPayload,
        Instant receivedAt) {
      if (!Objects.equals(armedProviderName, providerName)
          || !Objects.equals(armedExternalEventId, externalEventId)) {
        return;
      }

      armedProviderName = null;
      armedExternalEventId = null;
      transactionTemplate.executeWithoutResult(
          status ->
              paymentWebhookEventRepository.saveAndFlush(
                  new PaymentWebhookEvent(
                      UUID.randomUUID(),
                      null,
                      providerName,
                      externalEventId,
                      eventType,
                      signatureValid,
                      rawPayload,
                      receivedAt.minusSeconds(1))));
    }
  }
}
