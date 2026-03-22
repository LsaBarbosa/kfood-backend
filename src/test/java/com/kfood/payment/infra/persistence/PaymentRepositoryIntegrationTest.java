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
class PaymentRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private PaymentRepository paymentRepository;

  @Autowired private SalesOrderRepository salesOrderRepository;

  @Autowired private StoreRepository storeRepository;

  @Autowired private CustomerRepository customerRepository;

  @Test
  @DisplayName("should persist payment linked to order with pending initial status")
  void shouldPersistPaymentLinkedToOrder() {
    var store = storeRepository.saveAndFlush(store("loja-pagamento", "45.723.174/0001-10"));
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
                "Leave at the door"));

    var payment =
        Payment.create(
            UUID.randomUUID(), order, PaymentMethod.PIX, "mock-psp", "pix_123", "copy-and-paste");

    var savedPayment = paymentRepository.saveAndFlush(payment);

    assertThat(savedPayment.getId()).isNotNull();
    assertThat(savedPayment.getCreatedAt()).isNotNull();
    assertThat(savedPayment.getUpdatedAt()).isNotNull();
    assertThat(savedPayment.getOrder().getId()).isEqualTo(order.getId());
    assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(savedPayment.getAmount()).isEqualByComparingTo("57.50");
    assertThat(paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId())).hasSize(1);
    assertThat(paymentRepository.findByIdAndOrderStoreId(savedPayment.getId(), store.getId()))
        .isPresent();
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Store " + slug, slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
