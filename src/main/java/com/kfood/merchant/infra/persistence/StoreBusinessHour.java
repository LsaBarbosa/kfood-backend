package com.kfood.merchant.infra.persistence;

import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "store_hours")
public class StoreBusinessHour extends AuditableEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false, length = 20)
  private DayOfWeek dayOfWeek;

  @Column(name = "open_time")
  private LocalTime openTime;

  @Column(name = "close_time")
  private LocalTime closeTime;

  @Column(name = "is_closed", nullable = false)
  private boolean closed;

  protected StoreBusinessHour() {}

  private StoreBusinessHour(
      UUID id,
      Store store,
      DayOfWeek dayOfWeek,
      LocalTime openTime,
      LocalTime closeTime,
      boolean closed) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.store = Objects.requireNonNull(store, "store is required");
    this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek is required");
    this.openTime = openTime;
    this.closeTime = closeTime;
    this.closed = closed;
    validate();
  }

  public static StoreBusinessHour open(
      Store store, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime) {
    return new StoreBusinessHour(UUID.randomUUID(), store, dayOfWeek, openTime, closeTime, false);
  }

  public static StoreBusinessHour closed(Store store, DayOfWeek dayOfWeek) {
    return new StoreBusinessHour(UUID.randomUUID(), store, dayOfWeek, null, null, true);
  }

  @PrePersist
  void prePersist() {
    validate();
  }

  private void validate() {
    if (closed) {
      if (openTime != null || closeTime != null) {
        throw new IllegalArgumentException("closed day must not define openTime or closeTime");
      }
      return;
    }

    if (openTime == null || closeTime == null) {
      throw new IllegalArgumentException("openTime and closeTime are required for open day");
    }

    if (!openTime.isBefore(closeTime)) {
      throw new IllegalArgumentException("openTime must be before closeTime");
    }
  }

  public UUID getId() {
    return id;
  }

  public Store getStore() {
    return store;
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getOpenTime() {
    return openTime;
  }

  public LocalTime getCloseTime() {
    return closeTime;
  }

  public boolean isClosed() {
    return closed;
  }
}
