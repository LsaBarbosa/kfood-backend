package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderStatusHistoryTest {

  @Test
  void shouldCreateAuditEntryAndNormalizeReason() {
    var id = UUID.randomUUID();
    var storeId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var changedAt = Instant.parse("2026-03-22T18:40:00Z");

    var history =
        OrderStatusHistory.create(
            id,
            storeId,
            orderId,
            OrderStatus.NEW,
            OrderStatus.PREPARING,
            actorUserId,
            changedAt,
            "  Order entered preparation  ");

    assertThat(history.getId()).isEqualTo(id);
    assertThat(history.getStoreId()).isEqualTo(storeId);
    assertThat(history.getOrderId()).isEqualTo(orderId);
    assertThat(history.getPreviousStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(history.getNewStatus()).isEqualTo(OrderStatus.PREPARING);
    assertThat(history.getActorUserId()).isEqualTo(actorUserId);
    assertThat(history.getChangedAt()).isEqualTo(changedAt);
    assertThat(history.getReason()).isEqualTo("Order entered preparation");
  }

  @Test
  void shouldStoreNullReasonWhenBlank() {
    var history =
        OrderStatusHistory.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            OrderStatus.PREPARING,
            OrderStatus.READY,
            UUID.randomUUID(),
            Instant.parse("2026-03-22T18:40:00Z"),
            "   ");

    assertThat(history.getReason()).isNull();
  }

  @Test
  void shouldRejectEqualStatuses() {
    assertThatThrownBy(
            () ->
                OrderStatusHistory.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    OrderStatus.NEW,
                    OrderStatus.NEW,
                    UUID.randomUUID(),
                    Instant.parse("2026-03-22T18:40:00Z"),
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("previousStatus must differ from newStatus");
  }

  @Test
  void shouldRejectRequiredFields() {
    assertThatThrownBy(
            () ->
                OrderStatusHistory.create(
                    null,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    OrderStatus.NEW,
                    OrderStatus.PREPARING,
                    UUID.randomUUID(),
                    Instant.now(),
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("id is required");
    assertThatThrownBy(
            () ->
                OrderStatusHistory.create(
                    UUID.randomUUID(),
                    null,
                    UUID.randomUUID(),
                    OrderStatus.NEW,
                    OrderStatus.PREPARING,
                    UUID.randomUUID(),
                    Instant.now(),
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("storeId is required");
    assertThatThrownBy(
            () ->
                OrderStatusHistory.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    OrderStatus.NEW,
                    OrderStatus.PREPARING,
                    UUID.randomUUID(),
                    Instant.now(),
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("orderId is required");
    assertThatThrownBy(
            () ->
                OrderStatusHistory.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    OrderStatus.PREPARING,
                    UUID.randomUUID(),
                    Instant.now(),
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("previousStatus is required");
    assertThatThrownBy(
            () ->
                OrderStatusHistory.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    OrderStatus.NEW,
                    null,
                    UUID.randomUUID(),
                    Instant.now(),
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("newStatus is required");
    assertThatThrownBy(
            () ->
                OrderStatusHistory.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    OrderStatus.NEW,
                    OrderStatus.PREPARING,
                    null,
                    Instant.now(),
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("actorUserId is required");
    assertThatThrownBy(
            () ->
                OrderStatusHistory.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    OrderStatus.NEW,
                    OrderStatus.PREPARING,
                    UUID.randomUUID(),
                    null,
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("changedAt is required");
  }
}
