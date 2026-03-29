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
import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshotGateway;
import com.kfood.order.app.port.PublicOrderCommandPort;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.app.PaymentOutput;
import com.kfood.payment.app.RegisterCashPaymentUseCase;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.idempotency.IdempotencyService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.http.HttpStatus;

class CreatePublicOrderServiceTest {

  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-03-21T15:00:00Z"), ZoneOffset.UTC);
  }

  @Test
  void shouldCreateOrderSuccessfullyWithoutIdempotencyKey() {
    var commandPort = mock(PublicOrderCommandPort.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var validationService = mock(CheckoutCriticalValidationService.class);
    var publisher = mock(OrderCreatedPublisher.class);
    var idempotencyService = mock(IdempotencyService.class);
    var service =
        new CreatePublicOrderService(
            commandPort, quoteGateway, validationService, publisher, idempotencyService, fixedClock);
    var storeId = UUID.randomUUID();
    var quote = pickupQuote(storeId);
    var command =
        new CreatePublicOrderCommand(
            quote.quoteId(),
            quote.customerId(),
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            "Observacao",
            null);
    var output = output(UUID.randomUUID(), "PED-20260321-000001");

    when(commandPort.findStoreBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(new PublicOrderCommandPort.StoreReference(storeId)));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quote.quoteId()))
        .thenReturn(Optional.of(quote));
    when(commandPort.createOrder(storeId, command, quote)).thenReturn(output);

    var result = service.create("loja-do-bairro", null, command);

    assertThat(result).isEqualTo(output);
    verify(validationService).revalidate(storeId, quote);
    verify(commandPort).createOrder(storeId, command, quote);
    verify(publisher).publish(any(OrderCreatedEvent.class));
  }

  @Test
  void shouldExecuteInsideIdempotencyWhenKeyIsPresent() {
    var commandPort = mock(PublicOrderCommandPort.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var validationService = mock(CheckoutCriticalValidationService.class);
    var publisher = mock(OrderCreatedPublisher.class);
    var idempotencyService = mock(IdempotencyService.class);
    var service =
        new CreatePublicOrderService(
            commandPort, quoteGateway, validationService, publisher, idempotencyService, fixedClock);
    var storeId = UUID.randomUUID();
    var quote = pickupQuote(storeId);
    var command =
        new CreatePublicOrderCommand(
            quote.quoteId(),
            quote.customerId(),
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            null,
            null);
    var output = output(UUID.randomUUID(), "PED-20260321-000002");

    when(commandPort.findStoreBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(new PublicOrderCommandPort.StoreReference(storeId)));
    when(idempotencyService.execute(
            eq(storeId),
            eq("public-order-create"),
            eq("idem-123"),
            eq(command),
            eq(CreatePublicOrderOutput.class),
            any()))
        .thenAnswer(
            (InvocationOnMock invocation) ->
                ((Supplier<CreatePublicOrderOutput>) invocation.getArgument(5)).get());
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quote.quoteId()))
        .thenReturn(Optional.of(quote));
    when(commandPort.createOrder(storeId, command, quote)).thenReturn(output);

    var result = service.create("loja-do-bairro", "idem-123", command);

    assertThat(result).isEqualTo(output);
    verify(idempotencyService)
        .execute(
            eq(storeId),
            eq("public-order-create"),
            eq("idem-123"),
            eq(command),
            eq(CreatePublicOrderOutput.class),
            any());
  }

  @Test
  void shouldRegisterCashPaymentWhenMethodIsCash() {
    var commandPort = mock(PublicOrderCommandPort.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var validationService = mock(CheckoutCriticalValidationService.class);
    var publisher = mock(OrderCreatedPublisher.class);
    var idempotencyService = mock(IdempotencyService.class);
    var registerCashPaymentUseCase = mock(RegisterCashPaymentUseCase.class);
    var service =
        new CreatePublicOrderService(
            commandPort,
            quoteGateway,
            validationService,
            publisher,
            idempotencyService,
            registerCashPaymentUseCase,
            fixedClock);
    var storeId = UUID.randomUUID();
    var quote = pickupQuote(storeId);
    var orderId = UUID.randomUUID();
    var command =
        new CreatePublicOrderCommand(
            quote.quoteId(),
            quote.customerId(),
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.CASH,
            null,
            null);
    var output = output(orderId, "PED-20260321-000003");

    when(commandPort.findStoreBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(new PublicOrderCommandPort.StoreReference(storeId)));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quote.quoteId()))
        .thenReturn(Optional.of(quote));
    when(commandPort.createOrder(storeId, command, quote)).thenReturn(output);
    when(registerCashPaymentUseCase.execute(any()))
        .thenReturn(
            new PaymentOutput(
                UUID.randomUUID(),
                orderId,
                PaymentMethod.CASH,
                PaymentStatus.PENDING,
                output.totalAmount(),
                output.createdAt()));

    service.create("loja-do-bairro", null, command);

    var cashCaptor = ArgumentCaptor.forClass(com.kfood.payment.app.RegisterCashPaymentCommand.class);
    verify(registerCashPaymentUseCase).execute(cashCaptor.capture());
    assertThat(cashCaptor.getValue().orderId()).isEqualTo(orderId);
  }

  @Test
  void shouldNotRegisterCashPaymentWhenMethodIsPix() {
    var commandPort = mock(PublicOrderCommandPort.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var validationService = mock(CheckoutCriticalValidationService.class);
    var publisher = mock(OrderCreatedPublisher.class);
    var idempotencyService = mock(IdempotencyService.class);
    var registerCashPaymentUseCase = mock(RegisterCashPaymentUseCase.class);
    var service =
        new CreatePublicOrderService(
            commandPort,
            quoteGateway,
            validationService,
            publisher,
            idempotencyService,
            registerCashPaymentUseCase,
            fixedClock);
    var storeId = UUID.randomUUID();
    var quote = pickupQuote(storeId);
    var command =
        new CreatePublicOrderCommand(
            quote.quoteId(),
            quote.customerId(),
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            null,
            null);

    when(commandPort.findStoreBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(new PublicOrderCommandPort.StoreReference(storeId)));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quote.quoteId()))
        .thenReturn(Optional.of(quote));
    when(commandPort.createOrder(storeId, command, quote))
        .thenReturn(output(UUID.randomUUID(), "PED-20260321-000004"));

    service.create("loja-do-bairro", null, command);

    verify(registerCashPaymentUseCase, never()).execute(any());
  }

  @Test
  void shouldFailWhenStoreSlugDoesNotExist() {
    var service =
        new CreatePublicOrderService(
            mock(PublicOrderCommandPort.class),
            mock(CheckoutQuoteSnapshotGateway.class),
            mock(CheckoutCriticalValidationService.class),
            mock(OrderCreatedPublisher.class),
            mock(IdempotencyService.class),
            fixedClock);
    var command =
        new CreatePublicOrderCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            null,
            null);

    assertThatThrownBy(() -> service.create("loja-do-bairro", null, command))
        .isInstanceOf(com.kfood.merchant.app.StoreSlugNotFoundException.class);
  }

  @Test
  void shouldFailWhenQuoteIsMissing() {
    var commandPort = mock(PublicOrderCommandPort.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var service =
        new CreatePublicOrderService(
            commandPort,
            quoteGateway,
            mock(CheckoutCriticalValidationService.class),
            mock(OrderCreatedPublisher.class),
            mock(IdempotencyService.class),
            fixedClock);
    var storeId = UUID.randomUUID();
    var command =
        new CreatePublicOrderCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            null,
            null);

    when(commandPort.findStoreBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(new PublicOrderCommandPort.StoreReference(storeId)));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, command.quoteId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create("loja-do-bairro", null, command))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Quote not found or expired.")
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void shouldRejectWhenQuoteCustomerDiffersFromRequest() {
    var commandPort = mock(PublicOrderCommandPort.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var service =
        new CreatePublicOrderService(
            commandPort,
            quoteGateway,
            mock(CheckoutCriticalValidationService.class),
            mock(OrderCreatedPublisher.class),
            mock(IdempotencyService.class),
            fixedClock);
    var storeId = UUID.randomUUID();
    var quote = pickupQuote(storeId);
    var command =
        new CreatePublicOrderCommand(
            quote.quoteId(),
            UUID.randomUUID(),
            FulfillmentType.PICKUP,
            null,
            PaymentMethod.PIX,
            null,
            null);

    when(commandPort.findStoreBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(new PublicOrderCommandPort.StoreReference(storeId)));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quote.quoteId()))
        .thenReturn(Optional.of(quote));

    assertThatThrownBy(() -> service.create("loja-do-bairro", null, command))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Quote does not belong to the informed customer.")
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void shouldRejectWhenDeliveryAddressDiffersFromQuote() {
    var commandPort = mock(PublicOrderCommandPort.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var service =
        new CreatePublicOrderService(
            commandPort,
            quoteGateway,
            mock(CheckoutCriticalValidationService.class),
            mock(OrderCreatedPublisher.class),
            mock(IdempotencyService.class),
            fixedClock);
    var storeId = UUID.randomUUID();
    var quote = deliveryQuote(storeId);
    var command =
        new CreatePublicOrderCommand(
            quote.quoteId(),
            quote.customerId(),
            FulfillmentType.DELIVERY,
            UUID.randomUUID(),
            PaymentMethod.PIX,
            null,
            null);

    when(commandPort.findStoreBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(new PublicOrderCommandPort.StoreReference(storeId)));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quote.quoteId()))
        .thenReturn(Optional.of(quote));

    assertThatThrownBy(() -> service.create("loja-do-bairro", null, command))
        .isInstanceOf(BusinessException.class)
        .hasMessage("addressId differs from the quote.")
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void shouldRejectWhenFulfillmentTypeDiffersFromQuote() {
    var commandPort = mock(PublicOrderCommandPort.class);
    var quoteGateway = mock(CheckoutQuoteSnapshotGateway.class);
    var service =
        new CreatePublicOrderService(
            commandPort,
            quoteGateway,
            mock(CheckoutCriticalValidationService.class),
            mock(OrderCreatedPublisher.class),
            mock(IdempotencyService.class),
            fixedClock);
    var storeId = UUID.randomUUID();
    var quote = pickupQuote(storeId);
    var command =
        new CreatePublicOrderCommand(
            quote.quoteId(),
            quote.customerId(),
            FulfillmentType.DELIVERY,
            UUID.randomUUID(),
            PaymentMethod.PIX,
            null,
            null);

    when(commandPort.findStoreBySlug("loja-do-bairro"))
        .thenReturn(Optional.of(new PublicOrderCommandPort.StoreReference(storeId)));
    when(quoteGateway.findValidByStoreIdAndQuoteId(storeId, quote.quoteId()))
        .thenReturn(Optional.of(quote));

    assertThatThrownBy(() -> service.create("loja-do-bairro", null, command))
        .isInstanceOf(BusinessException.class)
        .hasMessage("fulfillmentType differs from the quote.")
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  private CheckoutQuoteSnapshot pickupQuote(UUID storeId) {
    return new CheckoutQuoteSnapshot(
        UUID.randomUUID(),
        storeId,
        UUID.randomUUID(),
        FulfillmentType.PICKUP,
        null,
        new BigDecimal("50.00"),
        BigDecimal.ZERO.setScale(2),
        new BigDecimal("50.00"),
        List.of(
            new CheckoutQuoteItemSnapshot(
                UUID.randomUUID(),
                "Pizza Calabresa",
                new BigDecimal("50.00"),
                1,
                null,
                List.of())),
        OffsetDateTime.now(fixedClock).plusMinutes(10));
  }

  private CheckoutQuoteSnapshot deliveryQuote(UUID storeId) {
    return new CheckoutQuoteSnapshot(
        UUID.randomUUID(),
        storeId,
        UUID.randomUUID(),
        FulfillmentType.DELIVERY,
        UUID.randomUUID(),
        new BigDecimal("50.00"),
        new BigDecimal("7.50"),
        new BigDecimal("57.50"),
        List.of(
            new CheckoutQuoteItemSnapshot(
                UUID.randomUUID(),
                "Pizza Calabresa",
                new BigDecimal("50.00"),
                1,
                null,
                List.of())),
        OffsetDateTime.now(fixedClock).plusMinutes(10));
  }

  private CreatePublicOrderOutput output(UUID orderId, String orderNumber) {
    return new CreatePublicOrderOutput(
        orderId,
        orderNumber,
        OrderStatus.NEW,
        PaymentStatusSnapshot.PENDING,
        new BigDecimal("50.00"),
        BigDecimal.ZERO.setScale(2),
        new BigDecimal("50.00"),
        Instant.parse("2026-03-21T15:00:00Z"));
  }
}
