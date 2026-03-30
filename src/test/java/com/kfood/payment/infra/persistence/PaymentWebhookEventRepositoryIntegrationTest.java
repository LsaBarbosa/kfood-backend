package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
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

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class PaymentWebhookEventRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private PaymentWebhookEventRepository paymentWebhookEventRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

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
}
