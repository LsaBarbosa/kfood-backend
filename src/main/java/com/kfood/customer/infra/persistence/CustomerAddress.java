package com.kfood.customer.infra.persistence;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer_address")
public class CustomerAddress extends AuditableEntity {

  @Id private UUID id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @NotBlank @Size(max = 60) @Column(name = "label", nullable = false, length = 60)
  private String label;

  @NotBlank @Pattern(regexp = "^\\d{8}$") @Column(name = "zip_code", nullable = false, length = 8)
  private String zipCode;

  @NotBlank @Size(max = 160) @Column(name = "street", nullable = false, length = 160)
  private String street;

  @NotBlank @Size(max = 20) @Column(name = "number", nullable = false, length = 20)
  private String number;

  @NotBlank @Size(max = 100) @Column(name = "district", nullable = false, length = 100)
  private String district;

  @NotBlank @Size(max = 100) @Column(name = "city", nullable = false, length = 100)
  private String city;

  @NotBlank @Pattern(regexp = "^[A-Z]{2}$") @Column(name = "state", nullable = false, length = 2)
  private String state;

  @Size(max = 120) @Column(name = "complement", length = 120)
  private String complement;

  @Column(name = "main_address", nullable = false)
  private boolean mainAddress;

  protected CustomerAddress() {}

  public CustomerAddress(
      UUID id,
      Customer customer,
      String label,
      String zipCode,
      String street,
      String number,
      String district,
      String city,
      String state,
      String complement,
      boolean mainAddress) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.customer = Objects.requireNonNull(customer, "customer is required");
    update(label, zipCode, street, number, district, city, state, complement, mainAddress);
  }

  @PrePersist
  @PreUpdate
  void validateLifecycle() {
    validateBusinessRules();
  }

  public void update(
      String label,
      String zipCode,
      String street,
      String number,
      String district,
      String city,
      String state,
      String complement,
      boolean mainAddress) {
    this.label = normalizeRequired(label, "label is required");
    this.zipCode = normalizeZipCode(zipCode);
    this.street = normalizeRequired(street, "street is required");
    this.number = normalizeRequired(number, "number is required");
    this.district = normalizeRequired(district, "district is required");
    this.city = normalizeRequired(city, "city is required");
    this.state = normalizeState(state);
    this.complement = normalizeNullable(complement);
    this.mainAddress = mainAddress;
    validateBusinessRules();
  }

  public void unsetMainAddress() {
    mainAddress = false;
  }

  public UUID getId() {
    return id;
  }

  public Customer getCustomer() {
    return customer;
  }

  public String getLabel() {
    return label;
  }

  public String getZipCode() {
    return zipCode;
  }

  public String getStreet() {
    return street;
  }

  public String getNumber() {
    return number;
  }

  public String getDistrict() {
    return district;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getComplement() {
    return complement;
  }

  public boolean isMainAddress() {
    return mainAddress;
  }

  private void validateBusinessRules() {
    if (label.isBlank()) {
      throw new IllegalArgumentException("label must not be blank");
    }
    if (street.isBlank()) {
      throw new IllegalArgumentException("street must not be blank");
    }
    if (number.isBlank()) {
      throw new IllegalArgumentException("number must not be blank");
    }
    if (district.isBlank()) {
      throw new IllegalArgumentException("district must not be blank");
    }
    if (city.isBlank()) {
      throw new IllegalArgumentException("city must not be blank");
    }
    if (!zipCode.matches("^\\d{8}$")) {
      throw new IllegalArgumentException("zipCode must contain 8 digits");
    }
    if (!state.matches("^[A-Z]{2}$")) {
      throw new IllegalArgumentException("state must have 2 letters");
    }
  }

  private String normalizeRequired(String value, String message) {
    return normalize(Objects.requireNonNull(value, message));
  }

  private String normalizeZipCode(String value) {
    var normalized = normalizeRequired(value, "zipCode is required").replaceAll("\\D", "");
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("zipCode must contain 8 digits");
    }
    return normalized;
  }

  private String normalizeState(String value) {
    var normalized = normalizeRequired(value, "state is required").toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("state must have 2 letters");
    }
    return normalized;
  }

  private String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : normalize(value);
  }

  private String normalize(String value) {
    return value.trim();
  }
}
