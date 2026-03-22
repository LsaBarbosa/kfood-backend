package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentMethod;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class OrderStatusHistoryRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private OrderStatusHistoryRepository orderStatusHistoryRepository;

  @Autowired private SalesOrderRepository salesOrderRepository;

  @Autowired private StoreRepository storeRepository;

  @Autowired private CustomerRepository customerRepository;

  @Test
  @DisplayName("should persist status history and list latest first by order")
  void shouldPersistStatusHistoryAndListLatestFirstByOrder() {
    var store = storeRepository.saveAndFlush(store("loja-do-bairro", "45.723.174/0001-10"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com"));
    var order =
        salesOrderRepository.saveAndFlush(
            SalesOrder.create(
                UUID.randomUUID(),
                store,
                customer,
                FulfillmentType.DELIVERY,
                PaymentMethod.PIX,
                new BigDecimal("50.00"),
                new BigDecimal("7.50"),
                new BigDecimal("57.50"),
                null,
                null));

    var first =
        orderStatusHistoryRepository.saveAndFlush(
            OrderStatusHistory.create(
                UUID.randomUUID(),
                store.getId(),
                order.getId(),
                OrderStatus.NEW,
                OrderStatus.PREPARING,
                UUID.randomUUID(),
                Instant.parse("2026-03-22T18:00:00Z"),
                "Preparation started"));
    var second =
        orderStatusHistoryRepository.saveAndFlush(
            OrderStatusHistory.create(
                UUID.randomUUID(),
                store.getId(),
                order.getId(),
                OrderStatus.PREPARING,
                OrderStatus.READY,
                UUID.randomUUID(),
                Instant.parse("2026-03-22T18:10:00Z"),
                null));

    var history = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtDesc(order.getId());

    assertThat(history)
        .extracting(OrderStatusHistory::getId)
        .containsExactly(second.getId(), first.getId());
    assertThat(history.getFirst().getNewStatus()).isEqualTo(OrderStatus.READY);
    assertThat(history.getLast().getPreviousStatus()).isEqualTo(OrderStatus.NEW);
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
