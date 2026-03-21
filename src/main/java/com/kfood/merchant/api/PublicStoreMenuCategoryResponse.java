package com.kfood.merchant.api;

import java.util.List;
import java.util.UUID;

public record PublicStoreMenuCategoryResponse(
    UUID id, String name, List<PublicStoreMenuProductResponse> products) {}
