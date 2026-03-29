package com.kfood.order.app;

import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshotGateway;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.order.app.port.PublicOrderCommandPort;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.payment.app.RegisterCashPaymentCommand;
import com.kfood.payment.app.RegisterCashPaymentUseCase;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.idempotency.IdempotencyService;
import org.springframework.beans.factory.ObjectProvider;
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
  PublicOrderCommandPort.class
})
public class CreatePublicOrderService {

  private static final String IDEMPOTENCY_SCOPE = "public-order-create";

  private final PublicOrderCommandPort publicOrderCommandPort;
  private final CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway;
  private final CheckoutCriticalValidationService checkoutCriticalValidationService;
  private final OrderCreatedPublisher orderCreatedPublisher;
  private final IdempotencyService idempotencyService;
  private final RegisterCashPaymentUseCase registerCashPaymentUseCase;
  private final Clock clock;

  @Autowired
  public CreatePublicOrderService(
      PublicOrderCommandPort publicOrderCommandPort,
      CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway,
      CheckoutCriticalValidationService checkoutCriticalValidationService,
      OrderCreatedPublisher orderCreatedPublisher,
      IdempotencyService idempotencyService,
      ObjectProvider<RegisterCashPaymentUseCase> registerCashPaymentUseCaseProvider,
      Clock clock) {
    this.publicOrderCommandPort = publicOrderCommandPort;
    this.checkoutQuoteSnapshotGateway = checkoutQuoteSnapshotGateway;
    this.checkoutCriticalValidationService = checkoutCriticalValidationService;
    this.orderCreatedPublisher = orderCreatedPublisher;
    this.idempotencyService = idempotencyService;
    this.registerCashPaymentUseCase = registerCashPaymentUseCaseProvider.getIfAvailable();
    this.clock = clock;
  }

  CreatePublicOrderService(
      PublicOrderCommandPort publicOrderCommandPort,
      CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway,
      CheckoutCriticalValidationService checkoutCriticalValidationService,
      OrderCreatedPublisher orderCreatedPublisher,
      IdempotencyService idempotencyService,
      RegisterCashPaymentUseCase registerCashPaymentUseCase,
      Clock clock) {
    this.publicOrderCommandPort = publicOrderCommandPort;
    this.checkoutQuoteSnapshotGateway = checkoutQuoteSnapshotGateway;
    this.checkoutCriticalValidationService = checkoutCriticalValidationService;
    this.orderCreatedPublisher = orderCreatedPublisher;
    this.idempotencyService = idempotencyService;
    this.registerCashPaymentUseCase = registerCashPaymentUseCase;
    this.clock = clock;
  }

  CreatePublicOrderService(
      PublicOrderCommandPort publicOrderCommandPort,
      CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway,
      CheckoutCriticalValidationService checkoutCriticalValidationService,
      OrderCreatedPublisher orderCreatedPublisher,
      IdempotencyService idempotencyService,
      Clock clock) {
    this(
        publicOrderCommandPort,
        checkoutQuoteSnapshotGateway,
        checkoutCriticalValidationService,
        orderCreatedPublisher,
        idempotencyService,
        (RegisterCashPaymentUseCase) null,
        clock);
  }

  @Transactional
  public CreatePublicOrderOutput create(
      String storeSlug, String idempotencyKey, CreatePublicOrderCommand command) {
    var storeId =
        publicOrderCommandPort
            .findStoreBySlug(storeSlug.trim())
            .map(PublicOrderCommandPort.StoreReference::id)
            .orElseThrow(() -> new StoreSlugNotFoundException(storeSlug));
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return doCreate(storeId, command);
    }
    return idempotencyService.execute(
        storeId,
        IDEMPOTENCY_SCOPE,
        idempotencyKey,
        command,
        CreatePublicOrderOutput.class,
        () -> doCreate(storeId, command));
  }

  private CreatePublicOrderOutput doCreate(UUID storeId, CreatePublicOrderCommand command) {
    var quote =
        checkoutQuoteSnapshotGateway
            .findValidByStoreIdAndQuoteId(storeId, command.quoteId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Quote not found or expired.",
                        HttpStatus.NOT_FOUND));
    validateRequestMatchesQuote(command, quote);
    checkoutCriticalValidationService.revalidate(storeId, quote);

    var createdOrder = publicOrderCommandPort.createOrder(storeId, command, quote);
    if (command.paymentMethod() == PaymentMethod.CASH && registerCashPaymentUseCase != null) {
      registerCashPaymentUseCase.execute(new RegisterCashPaymentCommand(createdOrder.id()));
    }
    orderCreatedPublisher.publish(
        new OrderCreatedEvent(
            createdOrder.id(),
            storeId,
            createdOrder.orderNumber(),
            createdOrder.status(),
            createdOrder.totalAmount(),
            OffsetDateTime.now(clock)));
    return createdOrder;
  }

  private void validateRequestMatchesQuote(CreatePublicOrderCommand command, CheckoutQuoteSnapshot quote) {
    if (!quote.customerId().equals(command.customerId())) {
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
        && !java.util.Objects.equals(quote.addressId(), command.addressId())) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR, "addressId differs from the quote.", HttpStatus.BAD_REQUEST);
    }
  }
}
