package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import com.kfood.payment.infra.eventing.PaymentWebhookRegisteredEventListener;
import com.kfood.payment.infra.eventing.SpringPaymentWebhookRegisteredPublisher;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.payment.infra.persistence.PaymentWebhookEventPersistenceAdapter;
import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import com.kfood.payment.infra.persistence.PaymentWebhookPaymentAdapter;
import com.kfood.shared.config.AsyncConfig;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@ActiveProfiles("test")
@Import({
  TestJpaAuditingConfig.class,
  AsyncConfig.class,
  PaymentWebhookEventPersistenceAdapter.class,
  PaymentWebhookPaymentAdapter.class,
  RegisterPaymentWebhookUseCase.class,
  ProcessConfirmedPaymentWebhookUseCase.class,
  SpringPaymentWebhookRegisteredPublisher.class,
  PaymentWebhookRegisteredEventListener.class,
  PaymentWebhookAsyncProcessingIntegrationTest.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class PaymentWebhookAsyncProcessingIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private RegisterPaymentWebhookUseCase registerPaymentWebhookUseCase;
  @Autowired private PaymentWebhookEventRepository paymentWebhookEventRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private SalesOrderRepository salesOrderRepository;
  @Autowired private StoreRepository storeRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  @DisplayName("should register first and process payment confirmation after commit")
  void shouldRegisterFirstAndProcessPaymentConfirmationAfterCommit() {
    var fixture = inNewTransaction(status -> persistPaymentFixture("charge-123"));
    var command =
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-async-123",
              "eventType": "PAYMENT_CONFIRMED",
              "providerReference": "charge-123"
            }
            """,
            true);

    var returnedEvent = inNewTransaction(status -> registerPaymentWebhookUseCase.execute(command));

    assertThat(returnedEvent.getProcessingStatus())
        .isEqualTo(PaymentWebhookProcessingStatus.RECEIVED);
    assertThat(returnedEvent.getExternalEventId()).isEqualTo("evt-async-123");

    awaitUntilProcessed("evt-async-123");

    var processedEvent =
        inNewTransaction(
            status ->
                paymentWebhookEventRepository
                    .findByProviderNameAndExternalEventId("mock", "evt-async-123")
                    .orElseThrow());
    var confirmedPayment =
        inNewTransaction(status -> paymentRepository.findById(fixture.paymentId()).orElseThrow());
    var refreshedOrder =
        inNewTransaction(status -> salesOrderRepository.findById(fixture.orderId()).orElseThrow());

    assertThat(processedEvent.getProcessingStatus())
        .isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    assertThat(processedEvent.getProcessedAt()).isNotNull();
    assertThat(processedEvent.getPayment()).isNotNull();
    assertThat(processedEvent.getPayment().getId()).isEqualTo(fixture.paymentId());
    assertThat(confirmedPayment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(confirmedPayment.getConfirmedAt()).isNotNull();
    assertThat(refreshedOrder.getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PAID);
  }

  @Test
  @DisplayName("should keep confirmed webhook replay idempotent after asynchronous processing")
  void shouldKeepConfirmedWebhookReplayIdempotentAfterAsynchronousProcessing() {
    var fixture = inNewTransaction(status -> persistPaymentFixture("charge-replay"));
    var command =
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-async-replay",
              "eventType": "PAYMENT_CONFIRMED",
              "providerReference": "charge-replay"
            }
            """,
            true);

    inNewTransaction(status -> registerPaymentWebhookUseCase.execute(command));
    awaitUntilProcessed("evt-async-replay");

    var processedEventBeforeReplay =
        inNewTransaction(
            status ->
                paymentWebhookEventRepository
                    .findByProviderNameAndExternalEventId("mock", "evt-async-replay")
                    .orElseThrow());
    var processedAt = processedEventBeforeReplay.getProcessedAt();

    var replayResult = inNewTransaction(status -> registerPaymentWebhookUseCase.execute(command));

    assertThat(replayResult.getId()).isEqualTo(processedEventBeforeReplay.getId());
    assertThat(replayResult.getProcessingStatus())
        .isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    assertThat(countRows("evt-async-replay")).isEqualTo(1);
    assertThat(
            inNewTransaction(
                    status ->
                        paymentWebhookEventRepository
                            .findByProviderNameAndExternalEventId("mock", "evt-async-replay")
                            .orElseThrow())
                .getProcessedAt())
        .isEqualTo(processedAt);
    assertThat(
            inNewTransaction(
                    status -> paymentRepository.findById(fixture.paymentId()).orElseThrow())
                .getStatus())
        .isEqualTo(PaymentStatus.CONFIRMED);
  }

  private void awaitUntilProcessed(String externalEventId) {
    Instant deadline = Instant.now().plusSeconds(5);
    while (Instant.now().isBefore(deadline)) {
      var current =
          inNewTransaction(
              status ->
                  paymentWebhookEventRepository.findByProviderNameAndExternalEventId(
                      "mock", externalEventId));
      if (current.isPresent()
          && current.get().getProcessingStatus() == PaymentWebhookProcessingStatus.PROCESSED) {
        return;
      }
      sleepBriefly();
    }
    throw new AssertionError("Webhook event was not processed asynchronously in time");
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

  private <T> T inNewTransaction(
      org.springframework.transaction.support.TransactionCallback<T> action) {
    var template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return template.execute(action);
  }

  private void sleepBriefly() {
    try {
      Thread.sleep(100L);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for webhook processing", exception);
    }
  }

  private PaymentFixture persistPaymentFixture(String providerReference) {
    var order = salesOrderRepository.saveAndFlush(order(providerReference));
    var payment =
        paymentRepository.saveAndFlush(
            new Payment(
                UUID.randomUUID(),
                order,
                PaymentMethod.PIX,
                "mock",
                providerReference,
                PaymentStatus.PENDING,
                new BigDecimal("57.50"),
                "000201...",
                null,
                Instant.parse("2026-03-30T10:45:00Z")));
    return new PaymentFixture(payment.getId(), order.getId());
  }

  private SalesOrder order(String suffix) {
    var store =
        storeRepository.saveAndFlush(
            new Store(
                UUID.randomUUID(),
                "Loja do Bairro",
                "loja-webhook-async-" + suffix,
                "45.723.174/0001-10",
                "21999990000",
                "America/Sao_Paulo"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(),
                store,
                "Maria Silva",
                "21999990000",
                "maria+" + suffix + "@email.com"));
    return SalesOrder.create(
        UUID.randomUUID(),
        store,
        customer,
        FulfillmentType.DELIVERY,
        PaymentMethod.PIX,
        new BigDecimal("50.00"),
        new BigDecimal("7.50"),
        new BigDecimal("57.50"),
        null,
        null);
  }

  private record PaymentFixture(UUID paymentId, UUID orderId) {}

  @TestConfiguration
  static class TestConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-03-30T16:00:00Z"), ZoneOffset.UTC);
    }
  }
}
