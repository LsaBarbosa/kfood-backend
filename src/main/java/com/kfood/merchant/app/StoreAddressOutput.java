package com.kfood.merchant.app;

public record StoreAddressOutput(
    String zipCode, String street, String number, String district, String city, String state) {}
