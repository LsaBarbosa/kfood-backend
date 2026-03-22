package com.kfood.order.api;

import com.kfood.order.domain.FulfillmentType;
import com.kfood.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePublicOrderRequest(
    @NotNull UUID quoteId,
    @NotNull UUID customerId,
    @NotNull FulfillmentType fulfillmentType,
    UUID addressId,
    @NotNull PaymentMethod paymentMethod,
    String notes,
    OffsetDateTime scheduledFor) {}
