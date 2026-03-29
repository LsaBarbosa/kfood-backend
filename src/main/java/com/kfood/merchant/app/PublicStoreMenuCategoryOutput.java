package com.kfood.merchant.app;

import java.util.List;
import java.util.UUID;

public record PublicStoreMenuCategoryOutput(
    UUID id, String name, List<PublicStoreMenuProductOutput> products) {}
