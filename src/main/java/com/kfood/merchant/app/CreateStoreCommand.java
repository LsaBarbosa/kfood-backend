package com.kfood.merchant.app;

public record CreateStoreCommand(
    String name, String slug, String cnpj, String phone, String timezone) {}
