package com.kfood.checkout.api;

import com.kfood.checkout.app.CalculateCheckoutQuoteItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record QuoteCheckoutItemRequest(
    @NotNull UUID productId,
    @Positive int quantity,
    String notes,
    @Valid List<QuoteCheckoutItemOptionRequest> options) {

  public CalculateCheckoutQuoteItemCommand toCommand() {
    return new CalculateCheckoutQuoteItemCommand(
        productId,
        quantity,
        notes,
        options == null
            ? List.of()
            : options.stream().map(QuoteCheckoutItemOptionRequest::toCommand).toList());
  }
}
