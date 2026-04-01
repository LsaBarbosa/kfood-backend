package com.kfood.catalog.infra.persistence;

import com.kfood.catalog.app.selection.CatalogOptionGroupView;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "catalog_option_group")
public class CatalogOptionGroup extends AuditableEntity implements CatalogOptionGroupView {

  @Id private UUID id;

  @Column(name = "store_id", nullable = false)
  private UUID storeId;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private CatalogProduct product;

  @NotBlank @Size(max = 120) @Column(name = "name", nullable = false, length = 120)
  private String name;

  @PositiveOrZero @Column(name = "min_select", nullable = false)
  private int minSelect;

  @PositiveOrZero @Column(name = "max_select", nullable = false)
  private int maxSelect;

  @Column(name = "required", nullable = false)
  private boolean required;

  @Column(name = "active", nullable = false)
  private boolean active;

  @OneToMany(
      mappedBy = "optionGroup",
      cascade = jakarta.persistence.CascadeType.ALL,
      orphanRemoval = true)
  @OrderBy("sortOrder ASC, id ASC")
  private final List<CatalogOptionItem> items = new ArrayList<>();

  protected CatalogOptionGroup() {}

  public CatalogOptionGroup(
      UUID id,
      CatalogProduct product,
      String name,
      int minSelect,
      int maxSelect,
      boolean required,
      boolean active) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.product = Objects.requireNonNull(product, "product is required");
    this.storeId = product.getStore().getId();
    this.name = normalize(Objects.requireNonNull(name, "name is required"));
    this.minSelect = minSelect;
    this.maxSelect = maxSelect;
    this.required = required;
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

  public CatalogProduct getProduct() {
    return product;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public String getName() {
    return name;
  }

  public int getMinSelect() {
    return minSelect;
  }

  public int getMaxSelect() {
    return maxSelect;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isActive() {
    return active;
  }

  public List<CatalogOptionItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public void changeName(String name) {
    this.name = normalize(Objects.requireNonNull(name, "name is required"));
    validateBusinessRules();
  }

  public void changeSelectionRange(int minSelect, int maxSelect) {
    this.minSelect = minSelect;
    this.maxSelect = maxSelect;
    validateBusinessRules();
  }

  public void activate() {
    active = true;
  }

  public void deactivate() {
    active = false;
  }

  public void addItem(CatalogOptionItem item) {
    Objects.requireNonNull(item, "item is required");
    if (item.getOptionGroup() != this) {
      throw new IllegalArgumentException("item optionGroup must match current group");
    }
    items.add(item);
  }

  private void validateBusinessRules() {
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (storeId == null) {
      throw new IllegalArgumentException("storeId is required");
    }
    if (!storeId.equals(product.getStore().getId())) {
      throw new IllegalArgumentException("optionGroup store must match product store");
    }
    if (minSelect < 0) {
      throw new IllegalArgumentException("minSelect must be greater than or equal to zero");
    }
    if (maxSelect < minSelect) {
      throw new IllegalArgumentException("maxSelect must be greater than or equal to minSelect");
    }
  }

  private String normalize(String value) {
    return value.trim();
  }
}
