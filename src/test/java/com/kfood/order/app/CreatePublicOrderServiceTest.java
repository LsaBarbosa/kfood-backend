package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.checkout.app.CheckoutQuoteItemSnapshot;
import com.kfood.checkout.app.CheckoutQuoteOptionSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshotGateway;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.api.CreatePublicOrderRequest;
import com.kfood.order.api.CreatePublicOrderResponse;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.idempotency.IdempotencyService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CreatePublicOrderServiceTest {

  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-03-21T15:00:00Z"), ZoneOffset.UTC);
  }

  @Test
  void shouldCreateOrderSuccessfully() {
    var storeRepository = mock(StoreRepository.class);
    var customerRepository = mock(CustomerRepository.class);
    var customerAddressRepository = mock(CustomerAddressRepository.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var validationService = mock(CheckoutCriticalValidationService.class);
    var orderRepository = mock(SalesOrderRepository.class);
    var assignOrderNumberService = mock(AssignOrderNumberService.class);
    var orderCreatedPublisher = mock(OrderCreatedPublisher.class);
    var idempotencyService = mock(IdempotencyService.class);
    var service =
        new CreatePublicOrderService(
            storeRepository,
            customerRepository,
            customerAddressRepository,
            quoteGateway,
            validationService,
            orderRepository,
            assignOrderNumberService,
            orderCreatedPublisher,
            idempotencyService,
            fixedClock);

    var storeId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var quoteId = UUID.randomUUID();
    var store = mock(Store.class);
    when(store.getId()).thenReturn(storeId);
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn(customerId);
    var quote =
        new CheckoutQuoteSnapshot(
            quoteId,
            storeId,
            customerId,
            FulfillmentType.PICKUP,
            null,
            new BigDecimal("50.00"),
            BigDecimal.ZERO.setScale(2),
            new BigDecimal("50.00"),
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
            OffsetDateTime.now(fixedClock).plusMinutes(10));
    var request =
        new CreatePublicOrderRequest(
            quoteId,
            customerId,
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            "Observacao",
            null);
    var saved = mock(SalesOrder.class);
    when(saved.getId()).thenReturn(UUID.randomUUID());
    when(saved.getOrderNumber()).thenReturn("PED-20260321-000001");
    when(saved.getStatus()).thenReturn(OrderStatus.NEW);
    when(saved.getPaymentStatusSnapshot()).thenReturn(PaymentStatusSnapshot.PENDING);
    when(saved.getSubtotalAmount()).thenReturn(new BigDecimal("50.00"));
    when(saved.getDeliveryFeeAmount()).thenReturn(BigDecimal.ZERO.setScale(2));
    when(saved.getTotalAmount()).thenReturn(new BigDecimal("50.00"));
    when(saved.getCreatedAt()).thenReturn(Instant.now());

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customerId, storeId))
        .thenReturn(Optional.of(customer));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quoteId))
        .thenReturn(Optional.of(quote));
    when(orderRepository.save(any(SalesOrder.class))).thenReturn(saved);

    var response = service.create("loja-do-bairro", null, request);

    assertThat(response.orderNumber()).isEqualTo("PED-20260321-000001");
    assertThat(response.status()).isEqualTo(OrderStatus.NEW);
    assertThat(response.paymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
    verify(validationService).revalidate(store, quote);
    verify(assignOrderNumberService).assignIfMissing(any(SalesOrder.class));
    verify(orderCreatedPublisher).publish(any(OrderCreatedEvent.class));
  }

  @Test
  void shouldCreateScheduledOrderForFutureTimeKeepingStatusNew() {
    var storeRepository = mock(StoreRepository.class);
    var customerRepository = mock(CustomerRepository.class);
    var customerAddressRepository = mock(CustomerAddressRepository.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var validationService = mock(CheckoutCriticalValidationService.class);
    var orderRepository = mock(SalesOrderRepository.class);
    var assignOrderNumberService = mock(AssignOrderNumberService.class);
    var orderCreatedPublisher = mock(OrderCreatedPublisher.class);
    var idempotencyService = mock(IdempotencyService.class);
    var service =
        new CreatePublicOrderService(
            storeRepository,
            customerRepository,
            customerAddressRepository,
            quoteGateway,
            validationService,
            orderRepository,
            assignOrderNumberService,
            orderCreatedPublisher,
            idempotencyService,
            fixedClock);

    var storeId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var quoteId = UUID.randomUUID();
    var scheduledFor = OffsetDateTime.parse("2026-03-21T16:00:00Z");
    var store = mock(Store.class);
    when(store.getId()).thenReturn(storeId);
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn(customerId);
    var quote =
        new CheckoutQuoteSnapshot(
            quoteId,
            storeId,
            customerId,
            FulfillmentType.PICKUP,
            null,
            new BigDecimal("50.00"),
            BigDecimal.ZERO.setScale(2),
            new BigDecimal("50.00"),
            List.of(),
            OffsetDateTime.now(fixedClock).plusMinutes(10));
    var request =
        new CreatePublicOrderRequest(
            quoteId,
            customerId,
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            "Scheduled order",
            scheduledFor);
    var saved = mock(SalesOrder.class);
    when(saved.getId()).thenReturn(UUID.randomUUID());
    when(saved.getOrderNumber()).thenReturn("PED-20260321-000002");
    when(saved.getStatus()).thenReturn(OrderStatus.NEW);
    when(saved.getPaymentStatusSnapshot()).thenReturn(PaymentStatusSnapshot.PENDING);
    when(saved.getSubtotalAmount()).thenReturn(new BigDecimal("50.00"));
    when(saved.getDeliveryFeeAmount()).thenReturn(BigDecimal.ZERO.setScale(2));
    when(saved.getTotalAmount()).thenReturn(new BigDecimal("50.00"));
    when(saved.getCreatedAt()).thenReturn(Instant.now());

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customerId, storeId))
        .thenReturn(Optional.of(customer));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quoteId))
        .thenReturn(Optional.of(quote));
    when(orderRepository.save(any(SalesOrder.class))).thenReturn(saved);

    var response = service.create("loja-do-bairro", null, request);

    assertThat(response.status()).isEqualTo(OrderStatus.NEW);
    verify(orderRepository).save(any(SalesOrder.class));
  }

  @Test
  void shouldRejectScheduledOrderInThePast() {
    var storeRepository = mock(StoreRepository.class);
    var customerRepository = mock(CustomerRepository.class);
    var customerAddressRepository = mock(CustomerAddressRepository.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var validationService = mock(CheckoutCriticalValidationService.class);
    var orderRepository = mock(SalesOrderRepository.class);
    var assignOrderNumberService = mock(AssignOrderNumberService.class);
    var orderCreatedPublisher = mock(OrderCreatedPublisher.class);
    var idempotencyService = mock(IdempotencyService.class);
    var service =
        new CreatePublicOrderService(
            storeRepository,
            customerRepository,
            customerAddressRepository,
            quoteGateway,
            validationService,
            orderRepository,
            assignOrderNumberService,
            orderCreatedPublisher,
            idempotencyService,
            fixedClock);

    var storeId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var quoteId = UUID.randomUUID();
    var store = mock(Store.class);
    when(store.getId()).thenReturn(storeId);
    var customer = mock(Customer.class);
    when(customer.getId()).thenReturn(customerId);
    var quote =
        new CheckoutQuoteSnapshot(
            quoteId,
            storeId,
            customerId,
            FulfillmentType.PICKUP,
            null,
            new BigDecimal("50.00"),
            BigDecimal.ZERO.setScale(2),
            new BigDecimal("50.00"),
            List.of(),
            OffsetDateTime.now(fixedClock).plusMinutes(10));
    var request =
        new CreatePublicOrderRequest(
            quoteId,
            customerId,
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            "Scheduled order",
            OffsetDateTime.parse("2026-03-21T14:00:00Z"));

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customerId, storeId))
        .thenReturn(Optional.of(customer));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quoteId))
        .thenReturn(Optional.of(quote));

    assertThatThrownBy(() -> service.create("loja-do-bairro", null, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            exception -> {
              var businessException = (BusinessException) exception;
              assertThat(businessException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(businessException.getErrorCode())
                  .isEqualTo(com.kfood.shared.exceptions.ErrorCode.VALIDATION_ERROR);
            })
        .hasMessage("scheduledFor must be in the future.");

    verify(orderRepository, never()).save(any(SalesOrder.class));
  }

  @Test
  void shouldUseIdempotencyServiceWhenKeyIsProvided() {
    var storeRepository = mock(StoreRepository.class);
    var idempotencyService = mock(IdempotencyService.class);
    var store = mock(Store.class);
    var storeId = UUID.randomUUID();
    var request =
        new CreatePublicOrderRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            null,
            null);
    var expected =
        new CreatePublicOrderResponse(
            UUID.randomUUID(),
            "PED-20260321-000003",
            OrderStatus.NEW,
            PaymentStatusSnapshot.PENDING,
            new BigDecimal("50.00"),
            BigDecimal.ZERO,
            new BigDecimal("50.00"),
            Instant.now());
    when(store.getId()).thenReturn(storeId);
    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(idempotencyService.execute(
            eq(storeId),
            eq("public-order-create"),
            eq("idem-123"),
            eq(request),
            eq(CreatePublicOrderResponse.class),
            any()))
        .thenReturn(expected);
    var service =
        new CreatePublicOrderService(
            storeRepository,
            mock(CustomerRepository.class),
            mock(CustomerAddressRepository.class),
            mock(CheckoutQuoteSnapshotGateway.class),
            mock(CheckoutCriticalValidationService.class),
            mock(SalesOrderRepository.class),
            mock(AssignOrderNumberService.class),
            mock(OrderCreatedPublisher.class),
            idempotencyService,
            fixedClock);

    var response = service.create("loja-do-bairro", "idem-123", request);

    assertThat(response.orderNumber()).isEqualTo("PED-20260321-000003");
  }

  @Test
  void shouldRejectWhenCustomerIsMissing() {
    var storeRepository = mock(StoreRepository.class);
    var customerRepository = mock(CustomerRepository.class);
    var storeId = UUID.randomUUID();
    var store = mock(Store.class);
    when(store.getId()).thenReturn(storeId);
    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(any(), eq(storeId))).thenReturn(Optional.empty());
    var service =
        new CreatePublicOrderService(
            storeRepository,
            customerRepository,
            mock(CustomerAddressRepository.class),
            mock(CheckoutQuoteSnapshotGateway.class),
            mock(CheckoutCriticalValidationService.class),
            mock(SalesOrderRepository.class),
            mock(AssignOrderNumberService.class),
            mock(OrderCreatedPublisher.class),
            mock(IdempotencyService.class),
            fixedClock);

    assertThatThrownBy(
            () ->
                service.create(
                    "loja-do-bairro",
                    null,
                    new CreatePublicOrderRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        FulfillmentType.PICKUP,
                        null,
                        PaymentMethod.PIX,
                        null,
                        null)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Customer not found for this store.");
  }

  @Test
  void shouldRejectWhenQuoteBelongsToDifferentCustomer() {
    var storeRepository = mock(StoreRepository.class);
    var customerRepository = mock(CustomerRepository.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var service =
        new CreatePublicOrderService(
            storeRepository,
            customerRepository,
            mock(CustomerAddressRepository.class),
            quoteGateway,
            mock(CheckoutCriticalValidationService.class),
            mock(SalesOrderRepository.class),
            mock(AssignOrderNumberService.class),
            mock(OrderCreatedPublisher.class),
            mock(IdempotencyService.class),
            fixedClock);
    var storeId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    when(store.getId()).thenReturn(storeId);
    when(customer.getId()).thenReturn(customerId);
    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(customerRepository.findByIdAndStoreId(customerId, storeId))
        .thenReturn(Optional.of(customer));
    when(quoteGateway.findValidByStoreIdAndQuoteId(eq(storeId), any()))
        .thenReturn(
            Optional.of(
                new CheckoutQuoteSnapshot(
                    UUID.randomUUID(),
                    storeId,
                    UUID.randomUUID(),
                    FulfillmentType.PICKUP,
                    null,
                    new BigDecimal("50.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("50.00"),
                    List.of(),
                    OffsetDateTime.now(fixedClock).plusMinutes(10))));

    assertThatThrownBy(
            () ->
                service.create(
                    "loja-do-bairro",
                    null,
                    new CreatePublicOrderRequest(
                        UUID.randomUUID(),
                        customerId,
                        FulfillmentType.PICKUP,
                        null,
                        PaymentMethod.PIX,
                        null,
                        null)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Quote does not belong to the informed customer.");
  }
}
