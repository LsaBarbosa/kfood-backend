package com.kfood.merchant.api;

import java.util.List;
import java.util.UUID;

public record PublicStoreMenuOptionGroupResponse(
    UUID id,
    String name,
    int minSelect,
    int maxSelect,
    boolean required,
    List<PublicStoreMenuOptionItemResponse> items) {}
