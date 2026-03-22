package com.kfood.order.infra.numbering;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.app.AssignOrderNumberService;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
@Import({
  TestJpaAuditingConfig.class,
  DatabaseOrderNumberGenerator.class,
  AssignOrderNumberService.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class OrderNumberIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private AssignOrderNumberService assignOrderNumberService;

  @Autowired private SalesOrderRepository salesOrderRepository;

  @Autowired private StoreRepository storeRepository;

  @Autowired private CustomerRepository customerRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute(
        """
        create sequence if not exists sales_order_number_seq
            start with 1
            increment by 1
            minvalue 1
            no maxvalue
            cache 1
        """);
  }

  @Test
  void shouldGenerateUniqueOrderNumbers() {
    var store = storeRepository.saveAndFlush(store("loja-numero", "45.723.174/0001-10"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com"));
    var first =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("30.00"),
            new BigDecimal("5.00"),
            new BigDecimal("35.00"),
            null,
            null);
    var second =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            new BigDecimal("5.00"),
            new BigDecimal("45.00"),
            null,
            null);

    assignOrderNumberService.assignIfMissing(first);
    assignOrderNumberService.assignIfMissing(second);
    salesOrderRepository.saveAndFlush(first);
    salesOrderRepository.saveAndFlush(second);

    assertThat(first.getOrderNumber()).isNotBlank();
    assertThat(second.getOrderNumber()).isNotBlank();
    assertThat(first.getOrderNumber()).isNotEqualTo(second.getOrderNumber());
    assertThat(first.getOrderNumber()).matches("^PED-[0-9]{8}-[0-9]{6}$");
    assertThat(second.getOrderNumber()).matches("^PED-[0-9]{8}-[0-9]{6}$");
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
