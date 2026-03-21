package com.kfood.catalog.infra.persistence;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "catalog_product",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_catalog_product_store_id_id",
          columnNames = {"store_id", "id"})
    })
public class CatalogProduct extends AuditableEntity {

  @Id private UUID id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @Column(name = "store_id", nullable = false, insertable = false, updatable = false)
  private UUID storeId;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private CatalogCategory category;

  @NotBlank @Size(max = 160) @Column(name = "name", nullable = false, length = 160)
  private String name;

  @NotBlank @Size(max = 500) @Column(name = "description", nullable = false, length = 500)
  private String description;

  @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal basePrice;

  @Size(max = 500) @Column(name = "image_url", length = 500)
  private String imageUrl;

  @PositiveOrZero @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "paused", nullable = false)
  private boolean paused;

  @OneToMany(
      mappedBy = "product",
      orphanRemoval = true,
      cascade = jakarta.persistence.CascadeType.ALL)
  @OrderBy("dayOfWeek ASC, startTime ASC")
  private final List<CatalogProductAvailabilityWindow> availabilityWindows = new ArrayList<>();

  protected CatalogProduct() {}

  public CatalogProduct(
      UUID id,
      Store store,
      CatalogCategory category,
      String name,
      String description,
      BigDecimal basePrice,
      String imageUrl,
      int sortOrder,
      boolean active,
      boolean paused) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.store = Objects.requireNonNull(store, "store is required");
    this.category = Objects.requireNonNull(category, "category is required");
    this.name = normalize(Objects.requireNonNull(name, "name is required"));
    this.description = normalize(Objects.requireNonNull(description, "description is required"));
    this.basePrice = normalize(Objects.requireNonNull(basePrice, "basePrice is required"));
    this.imageUrl = normalizeNullable(imageUrl);
    this.sortOrder = sortOrder;
    this.active = active;
    this.paused = paused;
    validateBusinessRules();
  }

  @PrePersist
  void prePersist() {
    validateBusinessRules();
  }

  public UUID getId() {
    return id;
  }

  public Store getStore() {
    return store;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public CatalogCategory getCategory() {
    return category;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isPaused() {
    return paused;
  }

  public List<CatalogProductAvailabilityWindow> getAvailabilityWindows() {
    return Collections.unmodifiableList(availabilityWindows);
  }

  public void changeCategory(CatalogCategory category) {
    this.category = Objects.requireNonNull(category, "category is required");
    validateBusinessRules();
  }

  public void changeName(String name) {
    this.name = normalize(Objects.requireNonNull(name, "name is required"));
    validateBusinessRules();
  }

  public void changeDescription(String description) {
    this.description = normalize(Objects.requireNonNull(description, "description is required"));
    validateBusinessRules();
  }

  public void changeBasePrice(BigDecimal basePrice) {
    this.basePrice = normalize(Objects.requireNonNull(basePrice, "basePrice is required"));
    validateBusinessRules();
  }

  public void changeImageUrl(String imageUrl) {
    this.imageUrl = normalizeNullable(imageUrl);
  }

  public void changeSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
    validateBusinessRules();
  }

  public void activate() {
    active = true;
  }

  public void deactivate() {
    active = false;
  }

  public void pause() {
    paused = true;
  }

  public void resume() {
    paused = false;
  }

  public void replaceAvailabilityWindows(List<CatalogProductAvailabilityWindow> windows) {
    availabilityWindows.clear();
    if (windows != null) {
      for (var window : windows) {
        if (window == null) {
          throw new IllegalArgumentException("availabilityWindow must not be null");
        }
        if (window.getProduct() != this) {
          throw new IllegalArgumentException(
              "availabilityWindow product must match catalog product");
        }
        availabilityWindows.add(window);
      }
    }
  }

  private void validateBusinessRules() {
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (description.isBlank()) {
      throw new IllegalArgumentException("description must not be blank");
    }
    if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("basePrice must be greater than or equal to zero");
    }
    if (sortOrder < 0) {
      throw new IllegalArgumentException("sortOrder must be greater than or equal to zero");
    }
    if (!store.getId().equals(category.getStore().getId())) {
      throw new IllegalArgumentException("product store must match category store");
    }
  }

  private BigDecimal normalize(BigDecimal value) {
    return value.setScale(2);
  }

  private String normalize(String value) {
    return value.trim();
  }

  private String normalizeNullable(String value) {
    return value == null ? null : value.trim();
  }
}
