package com.kfood.merchant.app;

import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import java.util.UUID;

public record StoreOutput(
    UUID id,
    String name,
    String slug,
    String cnpj,
    String phone,
    String timezone,
    StoreCategory category,
    StoreAddressOutput address,
    StoreStatus status) {

  public StoreOutput(
      UUID id,
      String name,
      String slug,
      String cnpj,
      String phone,
      String timezone,
      StoreStatus status) {
    this(id, name, slug, cnpj, phone, timezone, null, null, status);
  }
}
