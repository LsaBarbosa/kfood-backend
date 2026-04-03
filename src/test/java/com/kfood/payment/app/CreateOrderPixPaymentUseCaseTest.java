package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.app.gateway.PaymentGatewayErrorType;
import com.kfood.payment.app.gateway.PaymentGatewayException;
import com.kfood.payment.app.gateway.PixChargeGatewayResponseValidator;
import com.kfood.payment.app.port.PaymentOrderLookupPort;
import com.kfood.payment.app.port.PaymentPersistencePort;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.idempotency.IdempotencyKeyEntry;
import com.kfood.shared.idempotency.IdempotencyKeyRepository;
import com.kfood.shared.idempotency.IdempotencyService;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class CreateOrderPixPaymentUseCaseTest {

  private static final String IDEMPOTENCY_SCOPE = "order-pix-payment-create";

  private final PaymentOrderLookupPort paymentOrderLookupPort = mock(PaymentOrderLookupPort.class);
  private final PaymentPersistencePort paymentPersistencePort = mock(PaymentPersistencePort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CreatePixChargeUseCase createPixChargeUseCase = mock(CreatePixChargeUseCase.class);
  private final PixChargeGatewayResponseValidator pixChargeGatewayResponseValidator =
      new PixChargeGatewayResponseValidator();
  private final IdempotencyKeyRepository idempotencyKeyRepository =
      mock(IdempotencyKeyRepository.class);
  private final IdempotencyService idempotencyService =
      createIdempotencyService(idempotencyKeyRepository);
  private final CreateOrderPixPaymentUseCase useCase =
      new CreateOrderPixPaymentUseCase(
          paymentOrderLookupPort,
          paymentPersistencePort,
          currentTenantProvider,
          createPixChargeUseCase,
          pixChargeGatewayResponseValidator,
          idempotencyService);

  @Test
  void shouldCreatePendingPixPaymentAndReturnExpectedPayload() {
    var order = order();
    var command =
        new CreateOrderPixPaymentCommand(
            order.getId(), new BigDecimal("57.50"), "mock", "idem-123");
    mockNewIdempotentExecution(order.getStoreId(), command.idempotencyKey());

    mockSuccessfulPixChargeCreation(
        order,
        command.amount(),
        new PixChargeOutput(
            "mock", "pix-ref-123", "000201mock", OffsetDateTime.parse("2099-01-01T00:30:00Z")));

    var result = useCase.execute(command);

    ArgumentCaptor<CreatePixChargeCommand> gatewayCommandCaptor =
        ArgumentCaptor.forClass(CreatePixChargeCommand.class);
    verify(createPixChargeUseCase).execute(gatewayCommandCaptor.capture());
    var gatewayCommand = gatewayCommandCaptor.getValue();

    assertThat(order.getPaymentMethodSnapshot()).isEqualTo(PaymentMethod.PIX);
    assertThat(order.getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(gatewayCommand.providerCode()).isEqualTo("mock");
    assertThat(gatewayCommand.orderId()).isEqualTo(order.getId());
    assertThat(gatewayCommand.amount()).isEqualByComparingTo("57.50");
    assertThat(gatewayCommand.idempotencyKey()).isEqualTo("idem-123");
    assertThat(gatewayCommand.correlationId()).isNotBlank();
    assertThat(gatewayCommand.description()).contains(order.getId().toString());
    assertThat(result.paymentId()).isNotNull();
    assertThat(result.orderId()).isEqualTo(order.getId());
    assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(result.providerReference()).isEqualTo("pix-ref-123");
    assertThat(result.qrCodePayload()).isEqualTo("000201mock");
    assertThat(result.expiresAt()).isEqualTo(OffsetDateTime.parse("2099-01-01T00:30:00Z"));
  }

  @Test
  void shouldReturnSameResponseWhenIdempotencyKeyIsReusedWithSamePayload() {
    var order = order();
    var command =
        new CreateOrderPixPaymentCommand(
            order.getId(), new BigDecimal("57.50"), "mock", "idem-replay");
    var storedEntry = mockNewIdempotentExecution(order.getStoreId(), command.idempotencyKey());

    mockSuccessfulPixChargeCreation(
        order,
        command.amount(),
        new PixChargeOutput(
            "mock", "pix-ref-123", "000201mock", OffsetDateTime.parse("2099-01-01T00:30:00Z")));

    var firstResult = useCase.execute(command);
    var secondResult = useCase.execute(command);

    assertThat(secondResult).isEqualTo(firstResult);
    assertThat(storedEntry.get()).isNotNull();
    assertThat(storedEntry.get().getResponseBody()).isNotNull();
    verify(paymentOrderLookupPort, times(1))
        .findOrderByIdAndStoreId(order.getId(), order.getStoreId());
    verify(paymentPersistencePort, times(1))
        .savePendingPixPayment(any(UUID.class), eq(order), eq(command.amount()));
    verify(createPixChargeUseCase, times(1)).execute(any(CreatePixChargeCommand.class));
  }

  @Test
  void shouldThrowConflictWhenIdempotencyKeyIsReusedWithDifferentPayload() {
    var order = order();
    var firstCommand =
        new CreateOrderPixPaymentCommand(
            order.getId(), new BigDecimal("57.50"), "mock", "idem-conflict");
    var secondCommand =
        new CreateOrderPixPaymentCommand(
            order.getId(), new BigDecimal("58.50"), "mock", "idem-conflict");

    mockNewIdempotentExecution(order.getStoreId(), firstCommand.idempotencyKey());
    mockSuccessfulPixChargeCreation(
        order,
        firstCommand.amount(),
        new PixChargeOutput(
            "mock", "pix-ref-123", "000201mock", OffsetDateTime.parse("2099-01-01T00:30:00Z")));

    useCase.execute(firstCommand);

    assertThatThrownBy(() -> useCase.execute(secondCommand))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable -> {
              var businessException = (BusinessException) throwable;
              assertThat(businessException.getErrorCode())
                  .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD);
              assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });
    verify(paymentOrderLookupPort, times(1))
        .findOrderByIdAndStoreId(order.getId(), order.getStoreId());
    verify(createPixChargeUseCase, times(1)).execute(any(CreatePixChargeCommand.class));
  }

  @Test
  void shouldBypassLocalIdempotencyWhenHeaderIsMissing() {
    var order = order();
    var command =
        new CreateOrderPixPaymentCommand(order.getId(), new BigDecimal("57.50"), "mock", null);

    mockSuccessfulPixChargeCreation(
        order,
        command.amount(),
        new PixChargeOutput(
            "mock", "pix-ref-123", "000201mock", OffsetDateTime.parse("2099-01-01T00:30:00Z")));

    var result = useCase.execute(command);

    var gatewayCommandCaptor = ArgumentCaptor.forClass(CreatePixChargeCommand.class);
    verify(createPixChargeUseCase).execute(gatewayCommandCaptor.capture());
    assertThat(gatewayCommandCaptor.getValue().idempotencyKey()).isNull();
    assertThat(result.providerReference()).isEqualTo("pix-ref-123");
    verifyNoInteractions(idempotencyKeyRepository);
  }

  @Test
  void shouldRejectInvalidProviderResponse() {
    var order = order();

    mockSuccessfulPixPaymentPersistence(order, new BigDecimal("57.50"));
    when(createPixChargeUseCase.execute(any(CreatePixChargeCommand.class)))
        .thenReturn(
            new PixChargeOutput(
                "mock", " ", "000201mock", OffsetDateTime.parse("2099-01-01T00:30:00Z")));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreateOrderPixPaymentCommand(
                        order.getId(), new BigDecimal("57.50"), "mock", null)))
        .isInstanceOf(PaymentGatewayException.class)
        .satisfies(
            throwable ->
                assertThat(((PaymentGatewayException) throwable).getErrorType())
                    .isEqualTo(PaymentGatewayErrorType.INVALID_REQUEST));

    assertThat(order.getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
  }

  @Test
  void shouldPropagateProviderTimeoutOrUnavailable() {
    var order = order();
    var exception =
        new PaymentGatewayException(
            "mock", PaymentGatewayErrorType.TIMEOUT, "Pix provider timed out");

    mockSuccessfulPixPaymentPersistence(order, new BigDecimal("57.50"));
    when(createPixChargeUseCase.execute(any(CreatePixChargeCommand.class))).thenThrow(exception);

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreateOrderPixPaymentCommand(
                        order.getId(), new BigDecimal("57.50"), "mock", null)))
        .isSameAs(exception);
  }

  @Test
  void shouldFailWhenOrderDoesNotExistForTenant() {
    var storeId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(paymentOrderLookupPort.findOrderByIdAndStoreId(orderId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreateOrderPixPaymentCommand(
                        orderId, new BigDecimal("57.50"), "mock", null)))
        .isInstanceOf(OrderNotFoundException.class);
  }

  private AtomicReference<IdempotencyKeyEntry> mockNewIdempotentExecution(
      UUID storeId, String idempotencyKey) {
    var storedEntry = new AtomicReference<IdempotencyKeyEntry>();
    when(idempotencyKeyRepository.findByStoreIdAndScopeAndKeyValue(
            storeId, IDEMPOTENCY_SCOPE, idempotencyKey))
        .thenAnswer(invocation -> Optional.ofNullable(storedEntry.get()));
    when(idempotencyKeyRepository.save(any(IdempotencyKeyEntry.class)))
        .thenAnswer(
            invocation -> {
              var entry = invocation.getArgument(0, IdempotencyKeyEntry.class);
              storedEntry.set(entry);
              return entry;
            });
    return storedEntry;
  }

  private void mockSuccessfulPixChargeCreation(
      SalesOrder order, BigDecimal amount, PixChargeOutput gatewayOutput) {
    mockSuccessfulPixPaymentPersistence(order, amount);
    when(createPixChargeUseCase.execute(any(CreatePixChargeCommand.class)))
        .thenReturn(gatewayOutput);
  }

  private void mockSuccessfulPixPaymentPersistence(SalesOrder order, BigDecimal amount) {
    when(currentTenantProvider.getRequiredStoreId()).thenReturn(order.getStoreId());
    when(paymentOrderLookupPort.findOrderByIdAndStoreId(order.getId(), order.getStoreId()))
        .thenReturn(Optional.of(order));
    when(paymentPersistencePort.savePendingPixPayment(any(UUID.class), eq(order), eq(amount)))
        .thenAnswer(
            invocation ->
                com.kfood.payment.infra.persistence.Payment.createPendingPix(
                    invocation.getArgument(0),
                    (SalesOrder) invocation.getArgument(1),
                    invocation.getArgument(2)));
  }

  private static SalesOrder order() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var customer =
        new Customer(UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com");
    return SalesOrder.create(
        UUID.randomUUID(),
        store,
        customer,
        FulfillmentType.DELIVERY,
        PaymentMethod.PIX,
        new BigDecimal("50.00"),
        new BigDecimal("7.50"),
        new BigDecimal("57.50"),
        null,
        null);
  }

  private static IdempotencyService createIdempotencyService(
      IdempotencyKeyRepository idempotencyKeyRepository) {
    try {
      var constructor =
          IdempotencyService.class.getDeclaredConstructor(
              IdempotencyKeyRepository.class, ObjectMapper.class);
      constructor.setAccessible(true);
      return constructor.newInstance(idempotencyKeyRepository, createIdempotencyObjectMapper());
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException("Unable to create IdempotencyService for test", exception);
    }
  }

  private static ObjectMapper createIdempotencyObjectMapper() {
    var offsetDateTimeModule = new SimpleModule();
    offsetDateTimeModule.addSerializer(OffsetDateTime.class, ToStringSerializer.instance);
    offsetDateTimeModule.addDeserializer(
        OffsetDateTime.class,
        new JsonDeserializer<>() {
          @Override
          public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context)
              throws IOException {
            var value = parser.getValueAsString();
            return value == null ? null : OffsetDateTime.parse(value);
          }
        });
    return new ObjectMapper().findAndRegisterModules().registerModule(offsetDateTimeModule);
  }
}
