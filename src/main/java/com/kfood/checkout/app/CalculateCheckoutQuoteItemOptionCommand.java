package com.kfood.checkout.app;

import java.util.UUID;

public record CalculateCheckoutQuoteItemOptionCommand(UUID optionItemId, int quantity) {}
