package com.kfood.order.api;

import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePixPaymentResponse(
    UUID paymentId,
    UUID orderId,
    PaymentMethod paymentMethod,
    PaymentStatus status,
    String providerReference,
    String qrCodePayload,
    OffsetDateTime expiresAt) {}
