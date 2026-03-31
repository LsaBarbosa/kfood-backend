package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class PaymentWebhookEventRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private PaymentWebhookEventRepository paymentWebhookEventRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private SalesOrderRepository salesOrderRepository;
  @Autowired private StoreRepository storeRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  @DisplayName("should persist and find webhook event by provider and external event id")
  void shouldPersistAndFindWebhookEventByProviderAndExternalEventId() {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            true,
            "{\"id\":\"evt-123\"}",
            Instant.parse("2026-03-30T10:15:00Z"));

    var savedEvent = paymentWebhookEventRepository.saveAndFlush(event);

    assertThat(savedEvent.getCreatedAt()).isNotNull();
    assertThat(savedEvent.getUpdatedAt()).isNotNull();
    assertThat(
            paymentWebhookEventRepository.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .isPresent()
        .get()
        .extracting(PaymentWebhookEvent::getId)
        .isEqualTo(savedEvent.getId());
  }

  @Test
  @DisplayName("should persist webhook event linked to payment when correlation already exists")
  void shouldPersistWebhookEventLinkedToPaymentWhenCorrelationAlreadyExists() {
    var savedOrder = salesOrderRepository.saveAndFlush(order());
    var payment =
        paymentRepository.saveAndFlush(
            new Payment(
                UUID.randomUUID(),
                savedOrder,
                PaymentMethod.PIX,
                "mock",
                "charge-123",
                PaymentStatus.PENDING,
                new BigDecimal("57.50"),
                "000201...",
                null,
                Instant.parse("2026-03-30T10:45:00Z")));
    var event =
        paymentWebhookEventRepository.saveAndFlush(
            new PaymentWebhookEvent(
                UUID.randomUUID(),
                payment,
                "mock",
                "evt-linked",
                "PAYMENT_CONFIRMED",
                true,
                "{\"id\":\"evt-linked\"}",
                Instant.parse("2026-03-30T10:15:00Z")));

    assertThat(
            paymentWebhookEventRepository.findByProviderNameAndExternalEventId(
                "mock", "evt-linked"))
        .isPresent()
        .get()
        .extracting(found -> found.getPayment().getId())
        .isEqualTo(payment.getId());
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select payment_id
                from payment_webhook_event
                where id = ?
                """,
                UUID.class,
                event.getId()))
        .isEqualTo(payment.getId());
  }

  @Test
  @DisplayName("should list webhook events by processing status ordered by received at")
  void shouldListWebhookEventsByProcessingStatusOrderedByReceivedAt() {
    var firstReceived =
        paymentWebhookEventRepository.saveAndFlush(
            new PaymentWebhookEvent(
                UUID.randomUUID(),
                null,
                "mock",
                "evt-001",
                null,
                true,
                "{\"id\":\"evt-001\"}",
                Instant.parse("2026-03-30T10:10:00Z")));
    paymentWebhookEventRepository.saveAndFlush(
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-002",
            null,
            true,
            "{\"id\":\"evt-002\"}",
            Instant.parse("2026-03-30T10:20:00Z")));
    var processed =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-003",
            null,
            true,
            "{\"id\":\"evt-003\"}",
            Instant.parse("2026-03-30T10:30:00Z"));
    processed.markProcessed(Instant.parse("2026-03-30T10:31:00Z"));
    paymentWebhookEventRepository.saveAndFlush(processed);

    assertThat(
            paymentWebhookEventRepository.findAllByProcessingStatusOrderByReceivedAtAsc(
                PaymentWebhookProcessingStatus.RECEIVED))
        .extracting(PaymentWebhookEvent::getId)
        .containsExactly(
            firstReceived.getId(),
            paymentWebhookEventRepository
                .findByProviderNameAndExternalEventId("mock", "evt-002")
                .orElseThrow()
                .getId());
    assertThat(
            paymentWebhookEventRepository.findAllByProcessingStatusOrderByReceivedAtAsc(
                PaymentWebhookProcessingStatus.PROCESSED))
        .extracting(PaymentWebhookEvent::getId)
        .containsExactly(processed.getId());
  }

  @Test
  @DisplayName("should reject duplicated provider and external event id")
  void shouldRejectDuplicatedProviderAndExternalEventId() {
    paymentWebhookEventRepository.saveAndFlush(
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            null,
            true,
            "{\"id\":\"evt-123\"}",
            Instant.parse("2026-03-30T10:15:00Z")));

    assertThatThrownBy(
            () ->
                paymentWebhookEventRepository.saveAndFlush(
                    new PaymentWebhookEvent(
                        UUID.randomUUID(),
                        null,
                        "mock",
                        "evt-123",
                        null,
                        false,
                        "{\"id\":\"evt-123\",\"retry\":true}",
                        Instant.parse("2026-03-30T10:16:00Z"))))
        .isInstanceOf(Exception.class);
  }

  @Test
  @DisplayName("should keep a single row after equivalent duplicate attempts")
  void shouldKeepSingleRowAfterEquivalentDuplicateAttempts() {
    try {
      inNewTransaction(
          status -> {
            paymentWebhookEventRepository.saveAndFlush(
                new PaymentWebhookEvent(
                    UUID.randomUUID(),
                    null,
                    "mock",
                    "evt-single-row",
                    null,
                    true,
                    "{\"id\":\"evt-single-row\"}",
                    Instant.parse("2026-03-30T10:15:00Z")));
            return null;
          });

      assertThatThrownBy(
              () ->
                  inNewTransaction(
                      status -> {
                        paymentWebhookEventRepository.saveAndFlush(
                            new PaymentWebhookEvent(
                                UUID.randomUUID(),
                                null,
                                "mock",
                                "evt-single-row",
                                null,
                                false,
                                "{\"id\":\"evt-single-row\",\"retry\":true}",
                                Instant.parse("2026-03-30T10:16:00Z")));
                        return null;
                      }))
          .isInstanceOf(Exception.class);

      Integer persistedRows =
          inNewTransaction(
              status ->
                  jdbcTemplate.queryForObject(
                      """
                      select count(*)
                      from payment_webhook_event
                      where provider_name = ? and external_event_id = ?
                      """,
                      Integer.class,
                      "mock",
                      "evt-single-row"));

      assertThat(persistedRows).isEqualTo(1);
    } finally {
      inNewTransaction(
          status -> {
            jdbcTemplate.update(
                """
                delete from payment_webhook_event
                where provider_name = ? and external_event_id = ?
                """,
                "mock",
                "evt-single-row");
            return null;
          });
    }
  }

  private <T> T inNewTransaction(TransactionCallback<T> action) {
    var template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return template.execute(action);
  }

  @Test
  @DisplayName("should persist processing status enum as string")
  void shouldPersistProcessingStatusEnumAsString() {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-processed",
            null,
            true,
            "{\"id\":\"evt-processed\"}",
            Instant.parse("2026-03-30T10:15:00Z"));
    event.markProcessed(Instant.parse("2026-03-30T10:16:00Z"));
    paymentWebhookEventRepository.saveAndFlush(event);

    var row =
        jdbcTemplate.queryForObject(
            """
            select processing_status
            from payment_webhook_event
            where id = ?
            """,
            String.class,
            event.getId());

    assertThat(row).isEqualTo("PROCESSED");
  }

  @Test
  @DisplayName("should persist failed processing status enum as string")
  void shouldPersistFailedProcessingStatusEnumAsString() {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-failed-processing",
            null,
            true,
            "{\"id\":\"evt-failed-processing\"}",
            Instant.parse("2026-03-30T10:15:00Z"));
    event.markFailedProcessing(Instant.parse("2026-03-30T10:16:00Z"));
    paymentWebhookEventRepository.saveAndFlush(event);

    var row =
        jdbcTemplate.queryForObject(
            """
            select processing_status
            from payment_webhook_event
            where id = ?
            """,
            String.class,
            event.getId());

    assertThat(row).isEqualTo("FAILED_PROCESSING");
  }

  private SalesOrder order() {
    var store =
        storeRepository.saveAndFlush(
            new Store(
                UUID.randomUUID(),
                "Loja do Bairro",
                "loja-webhook-" + UUID.randomUUID(),
                "45.723.174/0001-10",
                "21999990000",
                "America/Sao_Paulo"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com"));
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
}
