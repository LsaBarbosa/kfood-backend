package com.kfood.merchant.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicStoreMenuProductResponse(
    UUID id,
    String name,
    String description,
    BigDecimal basePrice,
    String imageUrl,
    boolean paused,
    List<PublicStoreMenuOptionGroupResponse> optionGroups) {}
