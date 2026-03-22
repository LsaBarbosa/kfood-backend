package com.kfood.checkout.app;

import com.kfood.order.domain.FulfillmentType;
import java.util.List;
import java.util.UUID;

public record CalculateCheckoutQuoteCommand(
    UUID customerId,
    FulfillmentType fulfillmentType,
    UUID addressId,
    List<CalculateCheckoutQuoteItemCommand> items) {}
