package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentCommand(UUID orderId, PaymentMethod paymentMethod, BigDecimal amount) {}
