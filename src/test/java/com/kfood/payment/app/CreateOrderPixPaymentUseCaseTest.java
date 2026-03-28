package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.app.gateway.PaymentGatewayErrorType;
import com.kfood.payment.app.gateway.PaymentGatewayException;
import com.kfood.payment.app.gateway.PixChargeGatewayResponseValidator;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateOrderPixPaymentUseCaseTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CreatePixChargeUseCase createPixChargeUseCase = mock(CreatePixChargeUseCase.class);
  private final PixChargeGatewayResponseValidator pixChargeGatewayResponseValidator =
      new PixChargeGatewayResponseValidator();
  private final CreateOrderPixPaymentUseCase useCase =
      new CreateOrderPixPaymentUseCase(
          salesOrderRepository,
          paymentRepository,
          currentTenantProvider,
          createPixChargeUseCase,
          pixChargeGatewayResponseValidator);

  @Test
  void shouldCreatePendingPixPaymentAndReturnExpectedPayload() {
    var order = order();
    var command =
        new CreateOrderPixPaymentCommand(
            order.getId(), new BigDecimal("57.50"), "mock", "idem-123");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(order.getStore().getId());
    when(salesOrderRepository.findByIdAndStoreId(order.getId(), order.getStore().getId()))
        .thenReturn(Optional.of(order));
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(createPixChargeUseCase.execute(any(CreatePixChargeCommand.class)))
        .thenReturn(
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
  void shouldRejectInvalidProviderResponse() {
    var order = order();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(order.getStore().getId());
    when(salesOrderRepository.findByIdAndStoreId(order.getId(), order.getStore().getId()))
        .thenReturn(Optional.of(order));
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
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

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(order.getStore().getId());
    when(salesOrderRepository.findByIdAndStoreId(order.getId(), order.getStore().getId()))
        .thenReturn(Optional.of(order));
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
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
    when(salesOrderRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreateOrderPixPaymentCommand(
                        orderId, new BigDecimal("57.50"), "mock", null)))
        .isInstanceOf(OrderNotFoundException.class);
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
}
