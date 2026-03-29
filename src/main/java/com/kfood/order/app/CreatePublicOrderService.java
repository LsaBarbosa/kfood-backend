package com.kfood.order.app;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshotGateway;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderItem;
import com.kfood.order.infra.persistence.SalesOrderItemOption;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.app.RegisterCashPaymentCommand;
import com.kfood.payment.app.RegisterCashPaymentUseCase;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.idempotency.IdempotencyService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final IdempotencyService idempotencyService;
  private final RegisterCashPaymentUseCase registerCashPaymentUseCase;
  private final Clock clock;

  @Autowired
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
      RegisterCashPaymentUseCase registerCashPaymentUseCase,
      Clock clock) {
    this.storeRepository = storeRepository;
    this.customerRepository = customerRepository;
    this.customerAddressRepository = customerAddressRepository;
    this.checkoutQuoteSnapshotGateway = checkoutQuoteSnapshotGateway;
    this.checkoutCriticalValidationService = checkoutCriticalValidationService;
    this.salesOrderRepository = salesOrderRepository;
    this.assignOrderNumberService = assignOrderNumberService;
    this.orderCreatedPublisher = orderCreatedPublisher;
    this.idempotencyService = idempotencyService;
    this.registerCashPaymentUseCase = registerCashPaymentUseCase;
    this.clock = clock;
  }

  CreatePublicOrderService(
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
        idempotencyService,
        null,
        clock);
  }

  @Transactional
  public CreatePublicOrderOutput create(
      String storeSlug, String idempotencyKey, CreatePublicOrderCommand command) {
    var store =
        storeRepository
            .findBySlug(storeSlug.trim())
            .orElseThrow(() -> new StoreSlugNotFoundException(storeSlug));
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return doCreate(store.getId(), command);
    }
    return idempotencyService.execute(
        store.getId(),
        IDEMPOTENCY_SCOPE,
        idempotencyKey,
        command,
        CreatePublicOrderOutput.class,
        () -> doCreate(store.getId(), command));
  }

  private CreatePublicOrderOutput doCreate(UUID storeId, CreatePublicOrderCommand command) {
    var store = storeRepository.findById(storeId).orElseThrow();

    var customer =
        customerRepository
            .findByIdAndStoreId(command.customerId(), storeId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Customer not found for this store.",
                        HttpStatus.NOT_FOUND));
    var address =
        command.fulfillmentType() == FulfillmentType.PICKUP
            ? null
            : customerAddressRepository
                .findByIdAndCustomerId(command.addressId(), customer.getId())
                .orElseThrow(
                    () ->
                        new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Address not found for this customer.",
                            HttpStatus.NOT_FOUND));
    var quote =
        checkoutQuoteSnapshotGateway
            .findValidByStoreIdAndQuoteId(storeId, command.quoteId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Quote not found or expired.",
                        HttpStatus.NOT_FOUND));
    validateRequestMatchesQuote(
        command, quote, customer.getId(), address == null ? null : address.getId());
    checkoutCriticalValidationService.revalidate(store, quote);

    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            command.fulfillmentType(),
            command.paymentMethod(),
            quote.subtotalAmount(),
            quote.deliveryFeeAmount(),
            quote.totalAmount(),
            null,
            command.notes());
    if (address != null) {
      order.defineDeliveryAddressSnapshot(address);
    }
    try {
      order.defineSchedule(command.scheduledFor(), clock);
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
    if (command.paymentMethod() == PaymentMethod.CASH && registerCashPaymentUseCase != null) {
      registerCashPaymentUseCase.execute(new RegisterCashPaymentCommand(saved.getId()));
    }
    orderCreatedPublisher.publish(
        new OrderCreatedEvent(
            saved.getId(),
            store.getId(),
            saved.getOrderNumber(),
            saved.getStatus(),
            saved.getTotalAmount(),
            OffsetDateTime.now(clock)));
    return new CreatePublicOrderOutput(
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
      CreatePublicOrderCommand command,
      CheckoutQuoteSnapshot quote,
      UUID customerId,
      UUID addressId) {
    if (!quote.customerId().equals(customerId)) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Quote does not belong to the informed customer.",
          HttpStatus.BAD_REQUEST);
    }
    if (quote.fulfillmentType() != command.fulfillmentType()) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "fulfillmentType differs from the quote.",
          HttpStatus.BAD_REQUEST);
    }
    if (command.fulfillmentType() == FulfillmentType.DELIVERY
        && !java.util.Objects.equals(quote.addressId(), addressId)) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR, "addressId differs from the quote.", HttpStatus.BAD_REQUEST);
    }
  }
}
