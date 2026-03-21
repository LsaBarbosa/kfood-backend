package com.kfood.checkout.api;

import com.kfood.checkout.app.CalculateCheckoutQuoteItemOptionCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record QuoteCheckoutItemOptionRequest(@NotNull UUID optionItemId, @Positive int quantity) {

  public CalculateCheckoutQuoteItemOptionCommand toCommand() {
    return new CalculateCheckoutQuoteItemOptionCommand(optionItemId, quantity);
  }
}
