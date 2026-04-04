package com.kfood.payment.app;

import com.kfood.order.app.OrderNotFoundException;
import com.kfood.payment.app.gateway.CreatePixChargeResponse;
import com.kfood.payment.app.gateway.PixChargeGatewayResponseValidator;
import com.kfood.payment.app.port.PaymentOrder;
import com.kfood.payment.app.port.PaymentOrderLookupPort;
import com.kfood.payment.app.port.PaymentPersistencePort;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.shared.idempotency.IdempotencyService;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
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
  PixChargeGatewayResponseValidator.class,
  IdempotencyService.class
})
public class CreateOrderPixPaymentUseCase {

  private static final String IDEMPOTENCY_SCOPE = "order-pix-payment-create";

  private final PaymentOrderLookupPort paymentOrderLookupPort;
  private final PaymentPersistencePort paymentPersistencePort;
  private final CurrentTenantProvider currentTenantProvider;
  private final CreatePixChargeUseCase createPixChargeUseCase;
  private final PixChargeGatewayResponseValidator pixChargeGatewayResponseValidator;
  private final IdempotencyService idempotencyService;

  public CreateOrderPixPaymentUseCase(
      PaymentOrderLookupPort paymentOrderLookupPort,
      PaymentPersistencePort paymentPersistencePort,
      CurrentTenantProvider currentTenantProvider,
      CreatePixChargeUseCase createPixChargeUseCase,
      PixChargeGatewayResponseValidator pixChargeGatewayResponseValidator,
      IdempotencyService idempotencyService) {
    this.paymentOrderLookupPort = paymentOrderLookupPort;
    this.paymentPersistencePort = paymentPersistencePort;
    this.currentTenantProvider = currentTenantProvider;
    this.createPixChargeUseCase = createPixChargeUseCase;
    this.pixChargeGatewayResponseValidator = pixChargeGatewayResponseValidator;
    this.idempotencyService = idempotencyService;
  }

  @Transactional
  public OrderPixPaymentOutput execute(CreateOrderPixPaymentCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
      return doCreate(storeId, command);
    }
    return idempotencyService.execute(
        storeId,
        IDEMPOTENCY_SCOPE,
        command.idempotencyKey(),
        normalizedIdempotencyPayload(command),
        OrderPixPaymentOutput.class,
        () -> doCreate(storeId, command));
  }

  private OrderPixPaymentOutput doCreate(UUID storeId, CreateOrderPixPaymentCommand command) {
    var order =
        paymentOrderLookupPort
            .findOrderByIdAndStoreId(command.orderId(), storeId)
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

    order.markPaymentMethodSnapshot(PaymentMethod.PIX);
    order.markPaymentStatusSnapshot(PaymentStatusSnapshotMapper.from(PaymentStatus.PENDING));

    var payment =
        paymentPersistencePort.savePendingPixPayment(UUID.randomUUID(), order, command.amount());

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

  private String buildDescription(PaymentOrder order) {
    var orderReference =
        order.getOrderNumber() == null || order.getOrderNumber().isBlank()
            ? order.getId().toString()
            : order.getOrderNumber();
    return "Pix charge for order " + orderReference;
  }

  private CreateOrderPixPaymentIdempotencyPayload normalizedIdempotencyPayload(
      CreateOrderPixPaymentCommand command) {
    return new CreateOrderPixPaymentIdempotencyPayload(
        command.orderId(),
        normalizeAmount(command.amount()),
        normalizeProvider(command.provider()));
  }

  private BigDecimal normalizeAmount(BigDecimal amount) {
    return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
  }

  private String normalizeProvider(String provider) {
    return provider == null ? null : provider.trim().toLowerCase(Locale.ROOT);
  }

  private record CreateOrderPixPaymentIdempotencyPayload(
      UUID orderId, BigDecimal amount, String provider) {}
}
