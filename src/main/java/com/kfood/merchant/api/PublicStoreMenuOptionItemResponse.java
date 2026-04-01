package com.kfood.merchant.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicStoreMenuOptionItemResponse(
    UUID id, String name, BigDecimal extraPrice, int sortOrder) {}
