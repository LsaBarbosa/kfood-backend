package com.kfood.order.api;

import java.util.List;

public record ListOrdersResponse(
    List<ListOrdersResponseItem> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    List<String> sort) {}
