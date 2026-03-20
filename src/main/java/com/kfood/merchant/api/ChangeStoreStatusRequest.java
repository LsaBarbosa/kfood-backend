package com.kfood.merchant.api;

import com.kfood.merchant.domain.StoreStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStoreStatusRequest(@NotNull StoreStatus targetStatus) {}
