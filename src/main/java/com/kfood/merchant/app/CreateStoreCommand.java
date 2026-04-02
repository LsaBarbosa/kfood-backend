package com.kfood.merchant.app;

import com.kfood.merchant.domain.StoreCategory;

public record CreateStoreCommand(
    String name,
    String slug,
    String cnpj,
    String phone,
    String timezone,
    StoreCategory category,
    StoreAddressCommand address) {

  public CreateStoreCommand(String name, String slug, String cnpj, String phone, String timezone) {
    this(name, slug, cnpj, phone, timezone, null, null);
  }
}
