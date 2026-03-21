package com.kfood.catalog.infra.persistence;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "catalog_category")
public class CatalogCategory extends AuditableEntity {

  @Id private UUID id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @NotBlank @Size(max = 120) @Column(name = "name", nullable = false, length = 120)
  private String name;

  @PositiveOrZero @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "active", nullable = false)
  private boolean active;

  protected CatalogCategory() {}

  public CatalogCategory(UUID id, Store store, String name, int sortOrder, boolean active) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.store = Objects.requireNonNull(store, "store is required");
    this.name = normalize(Objects.requireNonNull(name, "name is required"));
    this.sortOrder = sortOrder;
    this.active = active;
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

  public String getName() {
    return name;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public boolean isActive() {
    return active;
  }

  public void changeName(String name) {
    this.name = normalize(Objects.requireNonNull(name, "name is required"));
    validateBusinessRules();
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

  private void validateBusinessRules() {
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (sortOrder < 0) {
      throw new IllegalArgumentException("sortOrder must be greater than or equal to zero");
    }
  }

  private String normalize(String value) {
    return value.trim();
  }
}
