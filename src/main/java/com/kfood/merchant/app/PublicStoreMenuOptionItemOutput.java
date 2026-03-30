package com.kfood.merchant.app;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicStoreMenuOptionItemOutput(
    UUID id, String name, BigDecimal extraPrice, int sortOrder) {}
