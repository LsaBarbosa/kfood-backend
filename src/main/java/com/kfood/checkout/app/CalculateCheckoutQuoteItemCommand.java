package com.kfood.checkout.app;

import java.util.List;
import java.util.UUID;

public record CalculateCheckoutQuoteItemCommand(
    UUID productId,
    int quantity,
    String notes,
    List<CalculateCheckoutQuoteItemOptionCommand> options) {}
