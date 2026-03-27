package com.kfood.payment.app;

import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.app.gateway.CreatePixChargeResponse;
import com.kfood.payment.app.gateway.PixChargeGatewayResponseValidator;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  SalesOrderRepository.class,
  PaymentRepository.class,
  CurrentTenantProvider.class,
  CreatePixChargeUseCase.class,
  PixChargeGatewayResponseValidator.class
})
public class CreateOrderPixPaymentUseCase {

  private final SalesOrderRepository salesOrderRepository;
  private final PaymentRepository paymentRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final CreatePixChargeUseCase createPixChargeUseCase;
  private final PixChargeGatewayResponseValidator pixChargeGatewayResponseValidator;

  public CreateOrderPixPaymentUseCase(
      SalesOrderRepository salesOrderRepository,
      PaymentRepository paymentRepository,
      CurrentTenantProvider currentTenantProvider,
      CreatePixChargeUseCase createPixChargeUseCase,
      PixChargeGatewayResponseValidator pixChargeGatewayResponseValidator) {
    this.salesOrderRepository = salesOrderRepository;
    this.paymentRepository = paymentRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.createPixChargeUseCase = createPixChargeUseCase;
    this.pixChargeGatewayResponseValidator = pixChargeGatewayResponseValidator;
  }

  @Transactional
  public OrderPixPaymentOutput execute(CreateOrderPixPaymentCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var order =
        salesOrderRepository
            .findByIdAndStoreId(command.orderId(), storeId)
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

    order.markPaymentMethodSnapshot(PaymentMethod.PIX);

    var payment =
        paymentRepository.save(
            Payment.createPendingPix(UUID.randomUUID(), order, command.amount()));

    var pixCharge =
        createPixChargeUseCase.execute(
            new CreatePixChargeCommand(
                command.provider(),
                payment.getId(),
                order.getId(),
                command.amount(),
                command.idempotencyKey(),
                payment.getId().toString(),
                buildDescription(order)));

    var validatedResponse =
        new CreatePixChargeResponse(
            pixCharge.providerName(),
            pixCharge.providerReference(),
            pixCharge.qrCodePayload(),
            pixCharge.expiresAt());
    pixChargeGatewayResponseValidator.ensureValid(command.provider(), validatedResponse);

    payment.attachPixChargeData(
        validatedResponse.providerName(),
        validatedResponse.providerReference(),
        validatedResponse.qrCodePayload(),
        validatedResponse.expiresAt());

    return new OrderPixPaymentOutput(
        payment.getId(),
        order.getId(),
        payment.getPaymentMethod(),
        payment.getStatus(),
        payment.getProviderReference(),
        payment.getQrCodePayload(),
        validatedResponse.expiresAt());
  }

  private String buildDescription(com.kfood.order.infra.persistence.SalesOrder order) {
    var orderReference =
        order.getOrderNumber() == null || order.getOrderNumber().isBlank()
            ? order.getId().toString()
            : order.getOrderNumber();
    return "Pix charge for order " + orderReference;
  }
}
