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
import com.kfood.payment.api.CreatePixPaymentResponse;
import com.kfood.payment.app.gateway.CreatePixChargeGatewayResult;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class CreatePixPaymentUseCaseTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final RequestPixChargeViaGatewayUseCase requestPixChargeViaGatewayUseCase =
      mock(RequestPixChargeViaGatewayUseCase.class);
  private final CreatePixPaymentUseCase useCase =
      new CreatePixPaymentUseCase(
          salesOrderRepository, paymentRepository, requestPixChargeViaGatewayUseCase);

  @Test
  void shouldReturnExpectedPixChargePayload() {
    var order = order(PaymentMethod.PIX, new BigDecimal("56.50"));
    var paymentId = UUID.randomUUID();
    var expiresAt = OffsetDateTime.parse("2030-03-22T12:15:00Z");

    when(salesOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenAnswer(
            invocation -> {
              var payment = invocation.getArgument(0, Payment.class);
              if (payment.getId() == null) {
                throw new IllegalStateException("Payment id must be generated before persistence.");
              }
              return payment;
            });
    when(requestPixChargeViaGatewayUseCase.execute(any(RequestPixChargeViaGatewayCommand.class)))
        .thenReturn(
            new CreatePixChargeGatewayResult(
                "default-psp", "psp_100", "0002012636mockpix100", expiresAt));

    CreatePixPaymentResponse response =
        useCase.execute(
            new CreatePixPaymentCommand(
                order.getId(), new BigDecimal("56.50"), "default-psp", "idem-1"));

    assertThat(response.paymentId()).isEqualTo(paymentId(response));
    assertThat(response.orderId()).isEqualTo(order.getId());
    assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(response.providerReference()).isEqualTo("psp_100");
    assertThat(response.qrCodePayload()).isEqualTo("0002012636mockpix100");
    assertThat(response.expiresAt()).isEqualTo(expiresAt);
  }

  @Test
  void shouldTreatInvalidProviderResponseAsControlledFailure() {
    var order = order(PaymentMethod.PIX, new BigDecimal("20.00"));

    when(salesOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Payment.class));
    when(requestPixChargeViaGatewayUseCase.execute(any(RequestPixChargeViaGatewayCommand.class)))
        .thenReturn(
            new CreatePixChargeGatewayResult(
                "default-psp", "", "", OffsetDateTime.now().minusMinutes(1)));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreatePixPaymentCommand(
                        order.getId(), new BigDecimal("20.00"), "default-psp", "idem-2")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode", "status")
        .containsExactly(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void shouldReturnControlledFailureOnProviderTimeout() {
    var order = order(PaymentMethod.PIX, new BigDecimal("30.00"));

    when(salesOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Payment.class));
    when(requestPixChargeViaGatewayUseCase.execute(any(RequestPixChargeViaGatewayCommand.class)))
        .thenThrow(
            new BusinessException(
                ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE,
                "Payment provider is unavailable.",
                HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreatePixPaymentCommand(
                        order.getId(), new BigDecimal("30.00"), "default-psp", "idem-3")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode", "status")
        .containsExactly(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void shouldFailWhenOrderDoesNotExist() {
    var missingOrderId = UUID.randomUUID();
    when(salesOrderRepository.findById(missingOrderId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreatePixPaymentCommand(
                        missingOrderId, new BigDecimal("10.00"), "default-psp", "idem-4")))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void shouldRejectPixAmountDifferentFromOrderTotal() {
    var order = order(PaymentMethod.PIX, new BigDecimal("42.00"));
    when(salesOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreatePixPaymentCommand(
                        order.getId(), new BigDecimal("41.99"), "default-psp", "idem-5")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode", "status")
        .containsExactly(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldRejectOrderWhenPaymentMethodIsNotPix() {
    var order = order(PaymentMethod.CASH, new BigDecimal("42.00"));
    when(salesOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreatePixPaymentCommand(
                        order.getId(), new BigDecimal("42.00"), "default-psp", "idem-6")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode", "status")
        .containsExactly(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldGenerateFallbackIdempotencyKeyWhenHeaderIsMissing() {
    var order = order(PaymentMethod.PIX, new BigDecimal("56.50"));
    when(salesOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Payment.class));
    when(requestPixChargeViaGatewayUseCase.execute(any(RequestPixChargeViaGatewayCommand.class)))
        .thenReturn(
            new CreatePixChargeGatewayResult(
                "default-psp",
                "psp_100",
                "0002012636mockpix100",
                OffsetDateTime.parse("2030-03-22T12:15:00Z")));

    useCase.execute(
        new CreatePixPaymentCommand(order.getId(), new BigDecimal("56.50"), "default-psp", " "));

    var captor = ArgumentCaptor.forClass(RequestPixChargeViaGatewayCommand.class);
    verify(requestPixChargeViaGatewayUseCase).execute(captor.capture());
    assertThat(captor.getValue().idempotencyKey()).startsWith("pix-");
  }

  private SalesOrder order(PaymentMethod paymentMethod, BigDecimal totalAmount) {
    return SalesOrder.create(
        UUID.randomUUID(),
        mock(Store.class),
        mock(Customer.class),
        FulfillmentType.DELIVERY,
        paymentMethod,
        totalAmount.subtract(new BigDecimal("7.50")),
        new BigDecimal("7.50"),
        totalAmount,
        null,
        null);
  }

  private UUID paymentId(CreatePixPaymentResponse response) {
    return response.paymentId();
  }
}
