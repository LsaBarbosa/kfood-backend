package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentStatus;
import java.util.UUID;

public record UpdatePaymentStatusCommand(UUID paymentId, PaymentStatus newStatus) {}
