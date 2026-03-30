package com.kfood.order.app;

import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailOutput(
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
    Customer customer,
    Address address,
    Payment payment,
    List<Item> items) {

  public record Customer(UUID id, String name, String phone, String email) {}

  public record Address(
      String label,
      String zipCode,
      String street,
      String number,
      String district,
      String city,
      String state,
      String complement) {}

  public record Payment(
      PaymentMethod paymentMethodSnapshot, PaymentStatusSnapshot paymentStatusSnapshot) {}

  public record Item(
      UUID id,
      UUID productId,
      String productNameSnapshot,
      BigDecimal unitPriceSnapshot,
      Integer quantity,
      BigDecimal totalItemAmount,
      String notes,
      List<ItemOption> options) {}

  public record ItemOption(
      UUID id,
      String optionNameSnapshot,
      BigDecimal extraPriceSnapshot,
      Integer quantity,
      BigDecimal totalExtraAmount) {}
}
