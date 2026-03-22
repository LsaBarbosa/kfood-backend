package com.kfood.order.app;

import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import java.time.LocalDate;

public record ListOrdersQuery(
    OrderStatus status, LocalDate dateFrom, LocalDate dateTo, FulfillmentType fulfillmentType) {}
