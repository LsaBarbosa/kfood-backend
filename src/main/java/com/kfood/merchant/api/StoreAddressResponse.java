package com.kfood.merchant.api;

public record StoreAddressResponse(
    String zipCode, String street, String number, String district, String city, String state) {}
