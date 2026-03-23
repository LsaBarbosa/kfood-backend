package com.kfood.order.app;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshotGateway;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.api.CreatePublicOrderRequest;
import com.kfood.order.api.CreatePublicOrderResponse;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderItem;
import com.kfood.order.infra.persistence.SalesOrderItemOption;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.idempotency.IdempotencyService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  CheckoutQuoteSnapshotGateway.class,
  CheckoutCriticalValidationService.class,
  SalesOrderRepository.class
})
public class CreatePublicOrderService {

  private static final String IDEMPOTENCY_SCOPE = "public-order-create";

  private final StoreRepository storeRepository;
  private final CustomerRepository customerRepository;
  private final CustomerAddressRepository customerAddressRepository;
  private final CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway;
  private final CheckoutCriticalValidationService checkoutCriticalValidationService;
  private final SalesOrderRepository salesOrderRepository;
  private final AssignOrderNumberService assignOrderNumberService;
  private final OrderCreatedPublisher orderCreatedPublisher;
  private final OrderPaymentRegistrar orderPaymentRegistrar;
  private final IdempotencyService idempotencyService;
  private final Clock clock;

  public CreatePublicOrderService(
      StoreRepository storeRepository,
      CustomerRepository customerRepository,
      CustomerAddressRepository customerAddressRepository,
      CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway,
      CheckoutCriticalValidationService checkoutCriticalValidationService,
      SalesOrderRepository salesOrderRepository,
      AssignOrderNumberService assignOrderNumberService,
      OrderCreatedPublisher orderCreatedPublisher,
      IdempotencyService idempotencyService,
      Clock clock) {
    this(
        storeRepository,
        customerRepository,
        customerAddressRepository,
        checkoutQuoteSnapshotGateway,
        checkoutCriticalValidationService,
        salesOrderRepository,
        assignOrderNumberService,
        orderCreatedPublisher,
        order -> {},
        idempotencyService,
        clock);
  }

  public CreatePublicOrderService(
      StoreRepository storeRepository,
      CustomerRepository customerRepository,
      CustomerAddressRepository customerAddressRepository,
      CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway,
      CheckoutCriticalValidationService checkoutCriticalValidationService,
      SalesOrderRepository salesOrderRepository,
      AssignOrderNumberService assignOrderNumberService,
      OrderCreatedPublisher orderCreatedPublisher,
      OrderPaymentRegistrar orderPaymentRegistrar,
      IdempotencyService idempotencyService,
      Clock clock) {
    this.storeRepository = storeRepository;
    this.customerRepository = customerRepository;
    this.customerAddressRepository = customerAddressRepository;
    this.checkoutQuoteSnapshotGateway = checkoutQuoteSnapshotGateway;
    this.checkoutCriticalValidationService = checkoutCriticalValidationService;
    this.salesOrderRepository = salesOrderRepository;
    this.assignOrderNumberService = assignOrderNumberService;
    this.orderCreatedPublisher = orderCreatedPublisher;
    this.orderPaymentRegistrar = orderPaymentRegistrar;
    this.idempotencyService = idempotencyService;
    this.clock = clock;
  }

  @Transactional
  public CreatePublicOrderResponse create(
      String storeSlug, String idempotencyKey, CreatePublicOrderRequest request) {
    var store =
        storeRepository
            .findBySlug(storeSlug.trim())
            .orElseThrow(() -> new StoreSlugNotFoundException(storeSlug));
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return doCreate(store, request);
    }
    return idempotencyService.execute(
        store.getId(),
        IDEMPOTENCY_SCOPE,
        idempotencyKey,
        request,
        CreatePublicOrderResponse.class,
        () -> doCreate(store, request));
  }

  private CreatePublicOrderResponse doCreate(
      com.kfood.merchant.infra.persistence.Store store, CreatePublicOrderRequest request) {
    var storeId = store.getId();

    var customer =
        customerRepository
            .findByIdAndStoreId(request.customerId(), storeId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Customer not found for this store.",
                        HttpStatus.NOT_FOUND));
    var address =
        request.fulfillmentType() == FulfillmentType.PICKUP
            ? null
            : customerAddressRepository
                .findByIdAndCustomerId(request.addressId(), customer.getId())
                .orElseThrow(
                    () ->
                        new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Address not found for this customer.",
                            HttpStatus.NOT_FOUND));
    var quote =
        checkoutQuoteSnapshotGateway
            .findValidByStoreIdAndQuoteId(storeId, request.quoteId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Quote not found or expired.",
                        HttpStatus.NOT_FOUND));
    validateRequestMatchesQuote(
        request, quote, customer.getId(), address == null ? null : address.getId());
    checkoutCriticalValidationService.revalidate(store, quote);

    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            request.fulfillmentType(),
            request.paymentMethod(),
            quote.subtotalAmount(),
            quote.deliveryFeeAmount(),
            quote.totalAmount(),
            null,
            request.notes());
    if (address != null) {
      order.defineDeliveryAddressSnapshot(address);
    }
    try {
      order.defineSchedule(request.scheduledFor(), clock);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "scheduledFor must be in the future.",
          HttpStatus.BAD_REQUEST);
    }
    for (var itemSnapshot : quote.items()) {
      var item =
          SalesOrderItem.create(
              UUID.randomUUID(),
              itemSnapshot.productId(),
              itemSnapshot.productNameSnapshot(),
              itemSnapshot.unitPriceSnapshot(),
              itemSnapshot.quantity(),
              itemSnapshot.notes());
      for (var optionSnapshot : itemSnapshot.options()) {
        item.addOption(
            SalesOrderItemOption.create(
                UUID.randomUUID(),
                optionSnapshot.optionNameSnapshot(),
                optionSnapshot.extraPriceSnapshot(),
                optionSnapshot.quantity()));
      }
      order.addItem(item);
    }

    assignOrderNumberService.assignIfMissing(order);
    var saved = salesOrderRepository.save(order);
    orderPaymentRegistrar.registerInitialPayment(saved);
    orderCreatedPublisher.publish(
        new OrderCreatedEvent(
            saved.getId(),
            store.getId(),
            saved.getOrderNumber(),
            saved.getStatus(),
            saved.getTotalAmount(),
            OffsetDateTime.now(clock)));
    return new CreatePublicOrderResponse(
        saved.getId(),
        saved.getOrderNumber(),
        saved.getStatus(),
        saved.getPaymentStatusSnapshot(),
        saved.getSubtotalAmount(),
        saved.getDeliveryFeeAmount(),
        saved.getTotalAmount(),
        saved.getCreatedAt());
  }

  private void validateRequestMatchesQuote(
      CreatePublicOrderRequest request,
      CheckoutQuoteSnapshot quote,
      UUID customerId,
      UUID addressId) {
    if (!quote.customerId().equals(customerId)) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Quote does not belong to the informed customer.",
          HttpStatus.BAD_REQUEST);
    }
    if (quote.fulfillmentType() != request.fulfillmentType()) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "fulfillmentType differs from the quote.",
          HttpStatus.BAD_REQUEST);
    }
    if (request.fulfillmentType() == FulfillmentType.DELIVERY
        && !java.util.Objects.equals(quote.addressId(), addressId)) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR, "addressId differs from the quote.", HttpStatus.BAD_REQUEST);
    }
  }
}
