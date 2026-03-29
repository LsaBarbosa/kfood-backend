package com.kfood.merchant.app;

import com.kfood.merchant.domain.StoreStatus;
import java.util.UUID;

public record StoreOutput(
    UUID id,
    String name,
    String slug,
    String cnpj,
    String phone,
    String timezone,
    StoreStatus status) {}
