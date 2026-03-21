package com.kfood.catalog.infra.persistence;

import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "catalog_option_item")
public class CatalogOptionItem extends AuditableEntity {

  @Id private UUID id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "option_group_id", nullable = false)
  private CatalogOptionGroup optionGroup;

  @NotBlank @Size(max = 120) @Column(name = "name", nullable = false, length = 120)
  private String name;

  @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(name = "extra_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal extraPrice;

  @Column(name = "active", nullable = false)
  private boolean active;

  @PositiveOrZero @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected CatalogOptionItem() {}

  public CatalogOptionItem(
      UUID id,
      CatalogOptionGroup optionGroup,
      String name,
      BigDecimal extraPrice,
      boolean active,
      int sortOrder) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.optionGroup = Objects.requireNonNull(optionGroup, "optionGroup is required");
    this.name = normalize(Objects.requireNonNull(name, "name is required"));
    this.extraPrice = normalize(Objects.requireNonNull(extraPrice, "extraPrice is required"));
    this.active = active;
    this.sortOrder = sortOrder;
    validateBusinessRules();
  }

  @PrePersist
  void prePersist() {
    validateBusinessRules();
  }

  public UUID getId() {
    return id;
  }

  public CatalogOptionGroup getOptionGroup() {
    return optionGroup;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getExtraPrice() {
    return extraPrice;
  }

  public boolean isActive() {
    return active;
  }

  public int getSortOrder() {
    return sortOrder;
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
    if (extraPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("extraPrice must be greater than or equal to zero");
    }
    if (sortOrder < 0) {
      throw new IllegalArgumentException("sortOrder must be greater than or equal to zero");
    }
  }

  private String normalize(String value) {
    return value.trim();
  }

  private BigDecimal normalize(BigDecimal value) {
    return value.setScale(2);
  }
}
