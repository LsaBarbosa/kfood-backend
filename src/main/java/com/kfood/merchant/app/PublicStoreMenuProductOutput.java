package com.kfood.merchant.app;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicStoreMenuProductOutput(
    UUID id,
    String name,
    String description,
    BigDecimal basePrice,
    String imageUrl,
    boolean paused,
    List<PublicStoreMenuOptionGroupOutput> optionGroups) {}
