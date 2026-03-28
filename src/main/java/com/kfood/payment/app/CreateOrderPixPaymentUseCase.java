package com.kfood.payment.app;

import com.kfood.order.app.OrderNotFoundException;
import com.kfood.payment.app.gateway.CreatePixChargeResponse;
import com.kfood.payment.app.gateway.PixChargeGatewayResponseValidator;
import com.kfood.payment.app.port.PaymentOrderLookupPort;
import com.kfood.payment.app.port.PaymentPersistencePort;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  PaymentOrderLookupPort.class,
  PaymentPersistencePort.class,
  CurrentTenantProvider.class,
  CreatePixChargeUseCase.class,
  PixChargeGatewayResponseValidator.class
})
public class CreateOrderPixPaymentUseCase {

  private final PaymentOrderLookupPort paymentOrderLookupPort;
  private final PaymentPersistencePort paymentPersistencePort;
  private final CurrentTenantProvider currentTenantProvider;
  private final CreatePixChargeUseCase createPixChargeUseCase;
  private final PixChargeGatewayResponseValidator pixChargeGatewayResponseValidator;

  public CreateOrderPixPaymentUseCase(
      PaymentOrderLookupPort paymentOrderLookupPort,
      PaymentPersistencePort paymentPersistencePort,
      CurrentTenantProvider currentTenantProvider,
      CreatePixChargeUseCase createPixChargeUseCase,
      PixChargeGatewayResponseValidator pixChargeGatewayResponseValidator) {
    this.paymentOrderLookupPort = paymentOrderLookupPort;
    this.paymentPersistencePort = paymentPersistencePort;
    this.currentTenantProvider = currentTenantProvider;
    this.createPixChargeUseCase = createPixChargeUseCase;
    this.pixChargeGatewayResponseValidator = pixChargeGatewayResponseValidator;
  }

  @Transactional
  public OrderPixPaymentOutput execute(CreateOrderPixPaymentCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var order =
        paymentOrderLookupPort
            .findOrderByIdAndStoreId(command.orderId(), storeId)
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

    order.markPaymentMethodSnapshot(PaymentMethod.PIX);
    order.markPaymentStatusSnapshot(PaymentStatusSnapshotMapper.from(PaymentStatus.PENDING));

    var payment =
        paymentPersistencePort.savePayment(
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
