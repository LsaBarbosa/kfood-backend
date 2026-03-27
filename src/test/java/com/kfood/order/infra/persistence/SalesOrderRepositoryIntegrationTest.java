package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
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

  @Autowired private CatalogProductRepository catalogProductRepository;

  @Autowired private CatalogCategoryRepository catalogCategoryRepository;

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
            PaymentMethod.PIX,
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
    assertThat(savedOrder.getPaymentMethodSnapshot()).isEqualTo(PaymentMethod.PIX);
    assertThat(savedOrder.getSubtotalAmount()).isEqualByComparingTo("50.00");
    assertThat(savedOrder.getDeliveryFeeAmount()).isEqualByComparingTo("7.50");
    assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("57.50");
    assertThat(salesOrderRepository.findByIdAndStoreId(savedOrder.getId(), store.getId()))
        .isPresent();
  }

  @Test
  @DisplayName("should persist order item with frozen snapshots and options")
  void shouldPersistOrderWithItemsAndOptions() {
    var store = storeRepository.saveAndFlush(store("loja-com-itens", "54.550.752/0001-55"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(UUID.randomUUID(), store, "Joao Silva", "21988887777", "joao@email.com"));
    var order =
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
            "Pedido com adicional");
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            null);
    item.addOption(
        SalesOrderItemOption.create(
            UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 1));
    order.addItem(item);

    var savedOrder = salesOrderRepository.saveAndFlush(order);

    assertThat(savedOrder.getItems()).hasSize(1);
    var savedItem = savedOrder.getItems().get(0);
    assertThat(savedItem.getProductNameSnapshot()).isEqualTo("Pizza Calabresa");
    assertThat(savedItem.getUnitPriceSnapshot()).isEqualByComparingTo("42.00");
    assertThat(savedItem.getTotalItemAmount()).isEqualByComparingTo("50.00");
    assertThat(savedItem.getOptions()).hasSize(1);
    assertThat(savedItem.getOptions().get(0).getOptionNameSnapshot()).isEqualTo("Borda Catupiry");
    assertThat(savedItem.getOptions().get(0).getExtraPriceSnapshot()).isEqualByComparingTo("8.00");
  }

  @Test
  @DisplayName("should keep item snapshot unchanged after product catalog update")
  void shouldKeepSnapshotUnchangedAfterCatalogUpdate() {
    var store = storeRepository.saveAndFlush(store("loja-snapshot", "54.550.752/0001-55"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(UUID.randomUUID(), store, "Ana Silva", "21977776666", "ana@email.com"));
    var category =
        catalogCategoryRepository.saveAndFlush(
            new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 1, true));
    var product =
        catalogProductRepository.saveAndFlush(
            new CatalogProduct(
                UUID.randomUUID(),
                store,
                category,
                "Pizza Calabresa",
                "Pizza com calabresa",
                new BigDecimal("42.00"),
                null,
                1,
                true,
                false));

    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.PICKUP,
            PaymentMethod.PIX,
            new BigDecimal("42.00"),
            BigDecimal.ZERO.setScale(2),
            new BigDecimal("42.00"),
            null,
            null);
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(), product.getId(), product.getName(), product.getBasePrice(), 1, null);
    order.addItem(item);
    var savedOrder = salesOrderRepository.saveAndFlush(order);

    product.changeName("Pizza Calabresa Promocional");
    product.changeBasePrice(new BigDecimal("49.90"));
    catalogProductRepository.saveAndFlush(product);

    var reloadedOrder = salesOrderRepository.findById(savedOrder.getId()).orElseThrow();
    var reloadedItem = reloadedOrder.getItems().get(0);

    assertThat(reloadedItem.getProductNameSnapshot()).isEqualTo("Pizza Calabresa");
    assertThat(reloadedItem.getUnitPriceSnapshot()).isEqualByComparingTo("42.00");
  }

  @Test
  @DisplayName("should exclude future scheduled orders from operational queue")
  void shouldExcludeFutureScheduledOrdersFromOperationalQueue() {
    var store = storeRepository.saveAndFlush(store("loja-fila", "45.723.174/0001-10"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), store, "Carlos Silva", "21977770000", "carlos@email.com"));
    var immediateOrder =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.PICKUP,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            BigDecimal.ZERO.setScale(2),
            new BigDecimal("40.00"),
            null,
            null);
    var scheduledOrder =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.PICKUP,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            BigDecimal.ZERO.setScale(2),
            new BigDecimal("50.00"),
            null,
            "Scheduled");
    scheduledOrder.defineSchedule(
        OffsetDateTime.parse("2026-03-21T16:00:00Z"),
        Clock.fixed(Instant.parse("2026-03-21T15:00:00Z"), java.time.ZoneOffset.UTC));

    salesOrderRepository.saveAndFlush(immediateOrder);
    salesOrderRepository.saveAndFlush(scheduledOrder);

    var queue =
        salesOrderRepository.findOperationalQueue(
            store.getId(),
            OrderStatus.NEW,
            null,
            null,
            null,
            OffsetDateTime.parse("2026-03-21T15:30:00Z"),
            PageRequest.of(0, 10));

    assertThat(queue.getContent()).extracting(SalesOrder::getId).contains(immediateOrder.getId());
    assertThat(queue.getContent())
        .extracting(SalesOrder::getId)
        .doesNotContain(scheduledOrder.getId());
  }

  @Test
  @DisplayName("should return only orders from the requested tenant store")
  void shouldReturnOnlyOrdersFromTheRequestedTenantStore() {
    var targetStore = storeRepository.saveAndFlush(store("loja-tenant-a", "45.723.174/0001-10"));
    var otherStore = storeRepository.saveAndFlush(store("loja-tenant-b", "54.550.752/0001-55"));
    var targetCustomer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), targetStore, "Maria Silva", "21999990000", "maria@email.com"));
    var otherCustomer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), otherStore, "Joao Silva", "21988887777", "joao@email.com"));
    var targetOrder =
        SalesOrder.create(
            UUID.randomUUID(),
            targetStore,
            targetCustomer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            null,
            null);
    var otherOrder =
        SalesOrder.create(
            UUID.randomUUID(),
            otherStore,
            otherCustomer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            new BigDecimal("8.00"),
            new BigDecimal("58.00"),
            null,
            null);

    salesOrderRepository.saveAndFlush(targetOrder);
    salesOrderRepository.saveAndFlush(otherOrder);

    var queue =
        salesOrderRepository.findOperationalQueue(
            targetStore.getId(),
            OrderStatus.NEW,
            null,
            null,
            null,
            OffsetDateTime.parse("2026-03-21T15:30:00Z"),
            PageRequest.of(0, 10));

    assertThat(queue.getContent())
        .extracting(SalesOrder::getId)
        .containsExactly(targetOrder.getId());
  }

  @Test
  @DisplayName("should load detailed order with customer items options and address snapshot")
  void shouldLoadDetailedOrderWithCustomerItemsOptionsAndAddressSnapshot() {
    var store = storeRepository.saveAndFlush(store("loja-detalhe", "45.723.174/0001-10"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com"));
    var address =
        new CustomerAddress(
            UUID.randomUUID(),
            customer,
            "Casa",
            "25000000",
            "Rua das Flores",
            "45",
            "Centro",
            "Mage",
            "RJ",
            "Ap 101",
            true);
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            new BigDecimal("6.50"),
            new BigDecimal("56.50"),
            null,
            "Tocar campainha");
    order.defineDeliveryAddressSnapshot(address);
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            "Sem cebola");
    item.addOption(
        SalesOrderItemOption.create(
            UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 1));
    order.addItem(item);
    var savedOrder = salesOrderRepository.saveAndFlush(order);

    var detailedOrder =
        salesOrderRepository
            .findDetailedByIdAndStoreId(savedOrder.getId(), store.getId())
            .orElseThrow();

    assertThat(detailedOrder.getCustomer().getName()).isEqualTo("Maria Silva");
    assertThat(detailedOrder.getItems()).hasSize(1);
    assertThat(detailedOrder.getItems().getFirst().getOptions()).hasSize(1);
    assertThat(detailedOrder.getDeliveryAddressStreet()).isEqualTo("Rua das Flores");
    assertThat(detailedOrder.getDeliveryAddressNumber()).isEqualTo("45");
  }

  @Test
  @DisplayName("should filter operational queue by fulfillment type and created at range")
  void shouldFilterOperationalQueueByFulfillmentTypeAndCreatedAtRange() {
    var store = storeRepository.saveAndFlush(store("loja-filtro", "54.550.752/0001-55"));
    var customer =
        customerRepository.saveAndFlush(
            new Customer(
                UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com"));
    var deliveryOrder =
        salesOrderRepository.saveAndFlush(
            SalesOrder.create(
                UUID.randomUUID(),
                store,
                customer,
                FulfillmentType.DELIVERY,
                PaymentMethod.PIX,
                new BigDecimal("50.00"),
                new BigDecimal("6.50"),
                new BigDecimal("56.50"),
                null,
                null));
    salesOrderRepository.saveAndFlush(
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.PICKUP,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            BigDecimal.ZERO,
            new BigDecimal("40.00"),
            null,
            null));

    var queue =
        salesOrderRepository.findOperationalQueue(
            store.getId(),
            OrderStatus.NEW,
            FulfillmentType.DELIVERY,
            Instant.EPOCH,
            Instant.now().plusSeconds(3600),
            OffsetDateTime.now(),
            PageRequest.of(0, 10));

    assertThat(queue.getContent())
        .extracting(SalesOrder::getId)
        .containsExactly(deliveryOrder.getId());
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
