package com.kfood.merchant.infra.persistence;

import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.validator.constraints.br.CNPJ;

@Entity
@Table(name = "store")
public class Store extends AuditableEntity {

  @Id private UUID id;

  @NotBlank @Size(max = 160) @Column(name = "name", nullable = false, length = 160)
  private String name;

  @NotBlank @Size(max = 120) @Pattern(
      regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
      message = "slug must contain only lowercase letters, numbers and hyphens")
  @Column(name = "slug", nullable = false, length = 120, unique = true)
  private String slug;

  @NotBlank @CNPJ
  @Column(name = "cnpj", nullable = false, length = 20)
  private String cnpj;

  @NotBlank @Pattern(regexp = "^\\d{10,15}$", message = "phone must contain between 10 and 15 digits") @Size(max = 20) @Column(name = "phone", nullable = false, length = 20)
  private String phone;

  @NotBlank @Size(max = 60) @Column(name = "timezone", nullable = false, length = 60)
  private String timezone;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", length = 40)
  private StoreCategory category;

  @Embedded private StoreAddress address;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private StoreStatus status;

  @Column(name = "hours_version", nullable = false)
  private int hoursVersion;

  @Column(name = "cash_payment_enabled", nullable = false)
  private boolean cashPaymentEnabled;

  protected Store() {}

  public Store(UUID id, String name, String slug, String cnpj, String phone, String timezone) {
    this(id, name, slug, cnpj, phone, timezone, null, null);
  }

  public Store(
      UUID id,
      String name,
      String slug,
      String cnpj,
      String phone,
      String timezone,
      StoreCategory category,
      StoreAddress address) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.name = normalize(Objects.requireNonNull(name, "name is required"));
    this.slug = normalize(Objects.requireNonNull(slug, "slug is required"));
    this.cnpj = normalize(Objects.requireNonNull(cnpj, "cnpj is required"));
    this.phone = normalize(Objects.requireNonNull(phone, "phone is required"));
    this.timezone = normalize(Objects.requireNonNull(timezone, "timezone is required"));
    this.category = category;
    this.address = address;
    status = StoreStatus.SETUP;
    hoursVersion = 0;
    cashPaymentEnabled = false;
  }

  @PrePersist
  void prePersist() {
    if (status == null) {
      status = StoreStatus.SETUP;
    }
    if (hoursVersion < 0) {
      hoursVersion = 0;
    }
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public String getCnpj() {
    return cnpj;
  }

  public String getPhone() {
    return phone;
  }

  public String getTimezone() {
    return timezone;
  }

  public StoreStatus getStatus() {
    return status;
  }

  public StoreCategory getCategory() {
    return category;
  }

  public StoreAddress getAddress() {
    return address;
  }

  public int getHoursVersion() {
    return hoursVersion;
  }

  public boolean isCashPaymentEnabled() {
    return cashPaymentEnabled;
  }

  public boolean isSetup() {
    return status == StoreStatus.SETUP;
  }

  public boolean isActive() {
    return status == StoreStatus.ACTIVE;
  }

  public boolean isSuspended() {
    return status == StoreStatus.SUSPENDED;
  }

  public void changeName(String name) {
    this.name = normalize(Objects.requireNonNull(name, "name is required"));
  }

  public void changeSlug(String slug) {
    this.slug = normalize(Objects.requireNonNull(slug, "slug is required"));
  }

  public void changeCnpj(String cnpj) {
    this.cnpj = normalize(Objects.requireNonNull(cnpj, "cnpj is required"));
  }

  public void changePhone(String phone) {
    this.phone = normalize(Objects.requireNonNull(phone, "phone is required"));
  }

  public void changeTimezone(String timezone) {
    this.timezone = normalize(Objects.requireNonNull(timezone, "timezone is required"));
  }

  public void changeCategory(StoreCategory category) {
    this.category = Objects.requireNonNull(category, "category is required");
  }

  public void changeAddress(StoreAddress address) {
    this.address = Objects.requireNonNull(address, "address is required");
  }

  public void incrementHoursVersion() {
    hoursVersion++;
  }

  public void enableCashPayment() {
    cashPaymentEnabled = true;
  }

  public void disableCashPayment() {
    cashPaymentEnabled = false;
  }

  public void activate() {
    if (status == StoreStatus.ACTIVE) {
      throw new StoreStatusTransitionException(status, StoreStatus.ACTIVE);
    }
    status = StoreStatus.ACTIVE;
  }

  public void suspend() {
    if (status != StoreStatus.ACTIVE) {
      throw new StoreStatusTransitionException(status, StoreStatus.SUSPENDED);
    }
    status = StoreStatus.SUSPENDED;
  }

  public boolean hasCategoryConfigured() {
    return category != null;
  }

  public boolean hasAddressConfigured() {
    return address != null && address.isComplete();
  }

  private String normalize(String value) {
    return value.trim();
  }
}
