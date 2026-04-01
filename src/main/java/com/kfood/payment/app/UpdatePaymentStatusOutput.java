package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.util.UUID;

public record UpdatePaymentStatusOutput(
    UUID paymentId,
    UUID orderId,
    PaymentStatus paymentStatus,
    PaymentStatusSnapshot orderPaymentStatusSnapshot) {}
