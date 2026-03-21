package com.kfood.checkout.api;

import com.kfood.checkout.app.CalculateCheckoutQuoteCommand;
import com.kfood.order.domain.FulfillmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record QuoteCheckoutRequest(
    @NotNull UUID customerId,
    @NotNull FulfillmentType fulfillmentType,
    UUID addressId,
    @NotEmpty @Valid List<QuoteCheckoutItemRequest> items) {

  public CalculateCheckoutQuoteCommand toCommand() {
    return new CalculateCheckoutQuoteCommand(
        customerId,
        fulfillmentType,
        addressId,
        items.stream().map(QuoteCheckoutItemRequest::toCommand).toList());
  }
}
