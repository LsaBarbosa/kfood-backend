package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
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
class SalesOrderRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private SalesOrderRepository salesOrderRepository;

  @Autowired private StoreRepository storeRepository;

  @Autowired private CustomerRepository customerRepository;

  @Test
  @DisplayName("should persist a valid order with store customer and frozen totals")
  void shouldPersistValidOrder() {
    var store = storeRepository.saveAndFlush(store("loja-do-bairro", "45.723.174/0001-10"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com"));
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            new BigDecimal("50.00"),
            new BigDecimal("7.50"),
            new BigDecimal("57.50"),
            null,
            "Leave at the door");

    var savedOrder = salesOrderRepository.saveAndFlush(order);

    assertThat(savedOrder.getId()).isNotNull();
    assertThat(savedOrder.getCreatedAt()).isNotNull();
    assertThat(savedOrder.getUpdatedAt()).isNotNull();
    assertThat(savedOrder.getStore().getId()).isEqualTo(store.getId());
    assertThat(savedOrder.getCustomer().getId()).isEqualTo(customer.getId());
    assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(savedOrder.getSubtotalAmount()).isEqualByComparingTo("50.00");
    assertThat(savedOrder.getDeliveryFeeAmount()).isEqualByComparingTo("7.50");
    assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("57.50");
    assertThat(salesOrderRepository.findByIdAndStoreId(savedOrder.getId(), store.getId()))
        .isPresent();
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
