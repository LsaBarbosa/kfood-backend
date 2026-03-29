package com.kfood.order.app;

import com.kfood.order.domain.FulfillmentType;
import com.kfood.payment.domain.PaymentMethod;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePublicOrderCommand(
    UUID quoteId,
    UUID customerId,
    FulfillmentType fulfillmentType,
    UUID addressId,
    PaymentMethod paymentMethod,
    String notes,
    OffsetDateTime scheduledFor) {}
