package com.kfood.customer.infra.persistence;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "customer",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_customer_store_phone",
          columnNames = {"store_id", "phone"}),
      @UniqueConstraint(
          name = "uk_customer_store_email",
          columnNames = {"store_id", "email"})
    })
public class Customer extends AuditableEntity {

  @Id private UUID id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @NotBlank @Size(max = 120) @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Size(max = 20) @Column(name = "phone", length = 20)
  private String phone;

  @Email @Size(max = 160) @Column(name = "email", length = 160)
  private String email;

  protected Customer() {}

  public Customer(UUID id, Store store, String name, String phone, String email) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.store = Objects.requireNonNull(store, "store is required");
    update(name, phone, email);
  }

  @PrePersist
  @PreUpdate
  void validateLifecycle() {
    validateBusinessRules();
  }

  public void update(String name, String phone, String email) {
    this.name = normalizeRequired(name, "name is required");
    this.phone = normalizePhone(phone);
    this.email = normalizeEmail(email);
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

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  private void validateBusinessRules() {
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (phone == null && email == null) {
      throw new IllegalArgumentException("phone or email must be informed");
    }
  }

  private String normalizeRequired(String value, String message) {
    return normalize(Objects.requireNonNull(value, message));
  }

  private String normalizePhone(String value) {
    return normalizeNullable(value);
  }

  private String normalizeEmail(String value) {
    var normalized = normalizeNullable(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : normalize(value);
  }

  private String normalize(String value) {
    return value.trim();
  }
}
