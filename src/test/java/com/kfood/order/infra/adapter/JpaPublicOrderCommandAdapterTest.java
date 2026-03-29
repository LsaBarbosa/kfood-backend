package com.kfood.order.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.checkout.app.CheckoutQuoteItemSnapshot;
import com.kfood.checkout.app.CheckoutQuoteOptionSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.app.AssignOrderNumberService;
import com.kfood.order.app.CreatePublicOrderCommand;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaPublicOrderCommandAdapterTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CustomerRepository customerRepository = mock(CustomerRepository.class);
  private final CustomerAddressRepository customerAddressRepository =
      mock(CustomerAddressRepository.class);
  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final AssignOrderNumberService assignOrderNumberService =
      mock(AssignOrderNumberService.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-21T15:00:00Z"), ZoneOffset.UTC);
  private final JpaPublicOrderCommandAdapter adapter =
      new JpaPublicOrderCommandAdapter(
          storeRepository,
          customerRepository,
          customerAddressRepository,
          salesOrderRepository,
          assignOrderNumberService,
          clock);

  @Test
  void shouldCreatePickupOrderAndReturnOutput() {
    var store = store();
    var customer = customer(store);
    var command = pickupCommand(customer.getId());
    var quote = pickupQuote(store.getId(), customer.getId());

    when(storeRepository.findBySlug(store.getSlug())).thenReturn(Optional.of(store));
    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(salesOrderRepository.save(any(SalesOrder.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(adapter.findStoreBySlug(store.getSlug()))
        .contains(
            new com.kfood.order.app.port.PublicOrderCommandPort.StoreReference(store.getId()));
    var output = adapter.createOrder(store.getId(), command, quote);

    assertThat(output.status()).isEqualTo(OrderStatus.NEW);
    assertThat(output.totalAmount()).isEqualByComparingTo("50.00");
    assertThat(output.paymentStatusSnapshot())
        .isEqualTo(com.kfood.payment.domain.PaymentStatusSnapshot.PENDING);
  }

  @Test
  void shouldRejectMissingDeliveryAddress() {
    var store = store();
    var customer = customer(store);
    var command =
        new CreatePublicOrderCommand(
            UUID.randomUUID(),
            customer.getId(),
            FulfillmentType.DELIVERY,
            UUID.randomUUID(),
            PaymentMethod.PIX,
            null,
            null);
    var quote =
        new CheckoutQuoteSnapshot(
            command.quoteId(),
            store.getId(),
            customer.getId(),
            FulfillmentType.DELIVERY,
            command.addressId(),
            new BigDecimal("50.00"),
            new BigDecimal("7.50"),
            new BigDecimal("57.50"),
            List.of(),
            OffsetDateTime.now(clock).plusMinutes(10));

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(customerAddressRepository.findByIdAndCustomerId(command.addressId(), customer.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.createOrder(store.getId(), command, quote))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void shouldRejectPastSchedule() {
    var store = store();
    var customer = customer(store);
    var command =
        new CreatePublicOrderCommand(
            UUID.randomUUID(),
            customer.getId(),
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            null,
            OffsetDateTime.parse("2026-03-21T14:00:00Z"));
    var quote = pickupQuote(store.getId(), customer.getId());

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));

    assertThatThrownBy(() -> adapter.createOrder(store.getId(), command, quote))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void shouldRejectWhenCustomerIsMissing() {
    var store = store();
    var command = pickupCommand(UUID.randomUUID());
    var quote = pickupQuote(store.getId(), command.customerId());

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(command.customerId(), store.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.createOrder(store.getId(), command, quote))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void shouldCreateDeliveryOrderWithAddressSnapshot() {
    var store = store();
    var customer = customer(store);
    var address =
        new CustomerAddress(
            UUID.randomUUID(),
            customer,
            "Casa",
            "20000000",
            "Rua das Flores",
            "100",
            "Centro",
            "Rio de Janeiro",
            "RJ",
            null,
            true);
    var command =
        new CreatePublicOrderCommand(
            UUID.randomUUID(),
            customer.getId(),
            FulfillmentType.DELIVERY,
            address.getId(),
            PaymentMethod.PIX,
            "Entregar na portaria",
            null);
    var quote =
        new CheckoutQuoteSnapshot(
            command.quoteId(),
            store.getId(),
            customer.getId(),
            FulfillmentType.DELIVERY,
            address.getId(),
            new BigDecimal("50.00"),
            new BigDecimal("7.50"),
            new BigDecimal("57.50"),
            List.of(
                new CheckoutQuoteItemSnapshot(
                    UUID.randomUUID(),
                    "Pizza Calabresa",
                    new BigDecimal("42.00"),
                    1,
                    "Sem cebola",
                    List.of(
                        new CheckoutQuoteOptionSnapshot(
                            "Borda Catupiry", new BigDecimal("8.00"), 1)))),
            OffsetDateTime.now(clock).plusMinutes(10));

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customer.getId(), store.getId()))
        .thenReturn(Optional.of(customer));
    when(customerAddressRepository.findByIdAndCustomerId(address.getId(), customer.getId()))
        .thenReturn(Optional.of(address));
    when(salesOrderRepository.save(any(SalesOrder.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var output = adapter.createOrder(store.getId(), command, quote);

    assertThat(output.totalAmount()).isEqualByComparingTo("57.50");
    assertThat(output.paymentStatusSnapshot())
        .isEqualTo(com.kfood.payment.domain.PaymentStatusSnapshot.PENDING);
  }

  private CreatePublicOrderCommand pickupCommand(UUID customerId) {
    return new CreatePublicOrderCommand(
        UUID.randomUUID(),
        customerId,
        FulfillmentType.PICKUP,
        null,
        PaymentMethod.PIX,
        "Observacao",
        null);
  }

  private CheckoutQuoteSnapshot pickupQuote(UUID storeId, UUID customerId) {
    return new CheckoutQuoteSnapshot(
        UUID.randomUUID(),
        storeId,
        customerId,
        FulfillmentType.PICKUP,
        null,
        new BigDecimal("50.00"),
        BigDecimal.ZERO,
        new BigDecimal("50.00"),
        List.of(
            new CheckoutQuoteItemSnapshot(
                UUID.randomUUID(),
                "Pizza Calabresa",
                new BigDecimal("42.00"),
                1,
                "Sem cebola",
                List.of(
                    new CheckoutQuoteOptionSnapshot("Borda Catupiry", new BigDecimal("8.00"), 1)))),
        OffsetDateTime.now(clock).plusMinutes(10));
  }

  private Store store() {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }

  private Customer customer(Store store) {
    return new Customer(UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com");
  }
}
