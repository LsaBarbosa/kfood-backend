package com.kfood.merchant.app;

public record StoreAddressCommand(
    String zipCode, String street, String number, String district, String city, String state) {}
