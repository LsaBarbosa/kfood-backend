package com.kfood.payment.app;

import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.api.CreatePixPaymentResponse;
import com.kfood.payment.app.gateway.CreatePixChargeGatewayResult;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  SalesOrderRepository.class,
  PaymentRepository.class,
  RequestPixChargeViaGatewayUseCase.class
})
public class CreatePixPaymentUseCase {

  private final SalesOrderRepository salesOrderRepository;
  private final PaymentRepository paymentRepository;
  private final RequestPixChargeViaGatewayUseCase requestPixChargeViaGatewayUseCase;

  public CreatePixPaymentUseCase(
      SalesOrderRepository salesOrderRepository,
      PaymentRepository paymentRepository,
      RequestPixChargeViaGatewayUseCase requestPixChargeViaGatewayUseCase) {
    this.salesOrderRepository = salesOrderRepository;
    this.paymentRepository = paymentRepository;
    this.requestPixChargeViaGatewayUseCase = requestPixChargeViaGatewayUseCase;
  }

  @Transactional
  public CreatePixPaymentResponse execute(CreatePixPaymentCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.orderId(), "orderId must not be null");
    Objects.requireNonNull(command.amount(), "amount must not be null");
    Objects.requireNonNull(command.provider(), "provider must not be null");

    var order =
        salesOrderRepository
            .findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

    if (order.getPaymentMethod() != PaymentMethod.PIX) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR, "Order payment method must be PIX.", HttpStatus.BAD_REQUEST);
    }

    if (order.getTotalAmount().compareTo(command.amount()) != 0) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Pix amount must match order total amount.",
          HttpStatus.BAD_REQUEST);
    }

    var payment =
        paymentRepository.saveAndFlush(
            Payment.create(UUID.randomUUID(), order, PaymentMethod.PIX, null, null, null));
    var gatewayResult =
        requestPixChargeViaGatewayUseCase.execute(
            new RequestPixChargeViaGatewayCommand(
                command.provider(),
                payment.getId(),
                order.getId(),
                payment.getAmount(),
                resolveIdempotencyKey(command, payment),
                "order-" + order.getId(),
                "Pix charge for order " + order.getId()));

    PixChargeResponseValidator.validate(command.provider(), gatewayResult);
    payment.attachPixCharge(
        gatewayResult.providerName(),
        gatewayResult.providerReference(),
        gatewayResult.qrCodePayload(),
        gatewayResult.expiresAt());
    var savedPayment = paymentRepository.saveAndFlush(payment);

    return toResponse(savedPayment, gatewayResult);
  }

  private String resolveIdempotencyKey(CreatePixPaymentCommand command, Payment payment) {
    if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
      return command.idempotencyKey().trim();
    }

    return "pix-" + payment.getId();
  }

  private CreatePixPaymentResponse toResponse(
      Payment payment, CreatePixChargeGatewayResult gatewayResult) {
    return new CreatePixPaymentResponse(
        payment.getId(),
        payment.getOrder().getId(),
        payment.getPaymentMethod(),
        payment.getStatus(),
        gatewayResult.providerReference(),
        gatewayResult.qrCodePayload(),
        gatewayResult.expiresAt());
  }
}
