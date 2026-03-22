package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentMethod;
import java.util.UUID;

public record CreatePaymentCommand(
    UUID orderId,
    PaymentMethod paymentMethod,
    String providerName,
    String providerReference,
    String qrCodePayload) {}
