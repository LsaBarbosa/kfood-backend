package com.kfood.merchant.app;

import java.util.List;
import java.util.UUID;

public record PublicStoreMenuOptionGroupOutput(
    UUID id,
    String name,
    int minSelect,
    int maxSelect,
    boolean required,
    List<PublicStoreMenuOptionItemOutput> items) {}
