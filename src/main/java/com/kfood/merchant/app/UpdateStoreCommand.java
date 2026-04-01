package com.kfood.merchant.app;

public record UpdateStoreCommand(
    String name, String slug, String cnpj, String phone, String timezone) {}
