package com.kfood.order.api;

import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
    UUID id,
    String orderNumber,
    OrderStatus status,
    FulfillmentType fulfillmentType,
    BigDecimal subtotalAmount,
    BigDecimal deliveryFeeAmount,
    BigDecimal totalAmount,
    String notes,
    OffsetDateTime scheduledFor,
    Instant createdAt,
    Instant updatedAt,
    CustomerDetail customer,
    AddressDetail address,
    PaymentDetail payment,
    List<ItemDetail> items) {

  public record CustomerDetail(UUID id, String name, String phone, String email) {}

  public record AddressDetail(
      String label,
      String zipCode,
      String street,
      String number,
      String district,
      String city,
      String state,
      String complement) {}

  public record PaymentDetail(
      PaymentMethod paymentMethodSnapshot, PaymentStatusSnapshot paymentStatusSnapshot) {}

  public record ItemDetail(
      UUID id,
      UUID productId,
      String productNameSnapshot,
      BigDecimal unitPriceSnapshot,
      Integer quantity,
      BigDecimal totalItemAmount,
      String notes,
      List<ItemOptionDetail> options) {}

  public record ItemOptionDetail(
      UUID id,
      String optionNameSnapshot,
      BigDecimal extraPriceSnapshot,
      Integer quantity,
      BigDecimal totalExtraAmount) {}
}
