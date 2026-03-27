package com.kfood.catalog.infra.persistence;

import com.kfood.catalog.app.availability.CatalogAvailabilityWindowView;
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
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "catalog_product_availability")
public class CatalogProductAvailabilityWindow extends AuditableEntity
    implements CatalogAvailabilityWindowView {

  @Id private UUID id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private CatalogProduct product;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false, length = 20)
  private DayOfWeek dayOfWeek;

  @NotNull @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @NotNull @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "active", nullable = false)
  private boolean active;

  protected CatalogProductAvailabilityWindow() {}

  public CatalogProductAvailabilityWindow(
      UUID id,
      CatalogProduct product,
      DayOfWeek dayOfWeek,
      LocalTime startTime,
      LocalTime endTime,
      boolean active) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.product = Objects.requireNonNull(product, "product is required");
    this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek is required");
    this.startTime = Objects.requireNonNull(startTime, "startTime is required");
    this.endTime = Objects.requireNonNull(endTime, "endTime is required");
    this.active = active;
    validate();
  }

  @PrePersist
  void prePersist() {
    validate();
  }

  private void validate() {
    if (!startTime.isBefore(endTime)) {
      throw new IllegalArgumentException("startTime must be before endTime");
    }
  }

  public boolean matches(DayOfWeek dayOfWeek, LocalTime localTime) {
    return active
        && this.dayOfWeek == dayOfWeek
        && !localTime.isBefore(startTime)
        && localTime.isBefore(endTime);
  }

  public UUID getId() {
    return id;
  }

  public CatalogProduct getProduct() {
    return product;
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public boolean isActive() {
    return active;
  }
}
