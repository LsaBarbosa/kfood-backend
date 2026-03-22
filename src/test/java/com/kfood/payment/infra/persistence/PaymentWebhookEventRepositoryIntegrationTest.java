package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class PaymentWebhookEventRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private PaymentWebhookEventRepository repository;

  @Test
  @DisplayName("should persist external event with unique identifier and received initial status")
  void shouldPersistExternalEvent() {
    var event =
        PaymentWebhookEvent.received(
            null,
            "MOCK_PSP",
            "evt_payment_confirmed_001",
            "{\"type\":\"payment.confirmed\",\"paymentId\":\"pay_001\"}");

    var saved = repository.saveAndFlush(event);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getProviderName()).isEqualTo("MOCK_PSP");
    assertThat(saved.getExternalEventId()).isEqualTo("evt_payment_confirmed_001");
    assertThat(saved.getRawPayload()).contains("payment.confirmed");
    assertThat(saved.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.WebhookProcessingStatus.RECEIVED);
    assertThat(saved.getReceivedAt()).isNotNull();
    assertThat(saved.getProcessedAt()).isNull();
  }

  @Test
  void shouldGenerateUniqueIdentifierWhenPersistingEvent() {
    var first =
        repository.saveAndFlush(
            PaymentWebhookEvent.received(
                null, "MOCK_PSP", "evt_unique_001", "{\"type\":\"payment.confirmed\"}"));
    var second =
        repository.saveAndFlush(
            PaymentWebhookEvent.received(
                null, "MOCK_PSP", "evt_unique_002", "{\"type\":\"payment.failed\"}"));

    assertThat(first.getId()).isNotNull();
    assertThat(second.getId()).isNotNull();
    assertThat(first.getId()).isNotEqualTo(second.getId());
  }

  @Test
  void shouldFindAndCheckExistenceByProviderAndExternalEventId() {
    repository.saveAndFlush(
        PaymentWebhookEvent.received(
            null, "MOCK_PSP", "evt_lookup_001", "{\"type\":\"payment.confirmed\"}"));

    assertThat(repository.findByProviderNameAndExternalEventId("MOCK_PSP", "evt_lookup_001"))
        .isPresent();
    assertThat(repository.existsByProviderNameAndExternalEventId("MOCK_PSP", "evt_lookup_001"))
        .isTrue();
  }

  @Test
  void shouldNotAllowDuplicateProviderAndExternalEventId() {
    repository.saveAndFlush(
        PaymentWebhookEvent.received(
            null, "MOCK_PSP", "evt_duplicate_001", "{\"type\":\"payment.confirmed\"}"));

    var duplicated =
        PaymentWebhookEvent.received(
            null, "MOCK_PSP", "evt_duplicate_001", "{\"type\":\"payment.confirmed\"}");

    assertThatThrownBy(() -> repository.saveAndFlush(duplicated))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
