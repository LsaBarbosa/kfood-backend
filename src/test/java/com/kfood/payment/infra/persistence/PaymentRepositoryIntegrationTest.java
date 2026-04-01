package com.kfood.payment.infra.persistence;

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

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class PaymentRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private PaymentRepository paymentRepository;
  @Autowired private SalesOrderRepository salesOrderRepository;
  @Autowired private StoreRepository storeRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("should persist payment linked to existing order")
  void shouldPersistPaymentLinkedToExistingOrder() {
    var savedOrder = salesOrderRepository.saveAndFlush(order());
    var payment =
        new Payment(
            UUID.randomUUID(),
            savedOrder,
            PaymentMethod.PIX,
            "pix-sandbox",
            "tx-123",
            PaymentStatus.PENDING,
            new BigDecimal("57.50"),
            "000201...",
            null,
            Instant.parse("2026-03-27T16:00:00Z"));

    var savedPayment = paymentRepository.saveAndFlush(payment);

    assertThat(savedPayment.getId()).isNotNull();
    assertThat(savedPayment.getCreatedAt()).isNotNull();
    assertThat(savedPayment.getUpdatedAt()).isNotNull();
    assertThat(savedPayment.getOrder().getId()).isEqualTo(savedOrder.getId());
    assertThat(savedPayment.getAmount()).isEqualByComparingTo("57.50");
    assertThat(paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(savedOrder.getId()))
        .extracting(Payment::getId)
        .containsExactly(savedPayment.getId());
  }

  @Test
  @DisplayName("should persist payment enums as strings")
  void shouldPersistPaymentEnumsAsStrings() {
    var savedOrder = salesOrderRepository.saveAndFlush(order());
    var payment =
        paymentRepository.saveAndFlush(
            new Payment(
                UUID.randomUUID(),
                savedOrder,
                PaymentMethod.CASH,
                "manual",
                "cash-1",
                PaymentStatus.CONFIRMED,
                new BigDecimal("57.50"),
                null,
                Instant.parse("2026-03-27T15:00:00Z"),
                Instant.parse("2026-03-27T16:00:00Z")));

    var row =
        jdbcTemplate.queryForObject(
            """
            select payment_method || '|' || status
            from payment
            where id = ?
            """,
            String.class,
            payment.getId());

    assertThat(row).isEqualTo("CASH|CONFIRMED");
  }

  @Test
  @DisplayName("should find payment by provider name and provider reference with order loaded")
  void shouldFindPaymentByProviderNameAndProviderReferenceWithOrderLoaded() {
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
                null));

    var result =
        paymentRepository.findDetailedByProviderNameAndProviderReference("mock", "charge-123");

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getId()).isEqualTo(payment.getId());
    assertThat(result.orElseThrow().getOrder().getId()).isEqualTo(savedOrder.getId());
  }

  private SalesOrder order() {
    var store =
        storeRepository.saveAndFlush(
            new Store(
                UUID.randomUUID(),
                "Loja do Bairro",
                "loja-do-bairro-" + UUID.randomUUID(),
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
