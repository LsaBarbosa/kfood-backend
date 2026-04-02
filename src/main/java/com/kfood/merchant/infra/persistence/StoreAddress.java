package com.kfood.merchant.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.Objects;

@Embeddable
public class StoreAddress {

  @Column(name = "address_zip_code", length = 8)
  private String zipCode;

  @Column(name = "address_street", length = 160)
  private String street;

  @Column(name = "address_number", length = 20)
  private String number;

  @Column(name = "address_district", length = 100)
  private String district;

  @Column(name = "address_city", length = 100)
  private String city;

  @Column(name = "address_state", length = 2)
  private String state;

  protected StoreAddress() {}

  public StoreAddress(
      String zipCode, String street, String number, String district, String city, String state) {
    this.zipCode = normalizeZipCode(zipCode);
    this.street = normalizeRequired(street, "street is required");
    this.number = normalizeRequired(number, "number is required");
    this.district = normalizeRequired(district, "district is required");
    this.city = normalizeRequired(city, "city is required");
    this.state = normalizeState(state);
    validateBusinessRules();
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

  public boolean isComplete() {
    return zipCode != null
        && street != null
        && number != null
        && district != null
        && city != null
        && state != null
        && !zipCode.isBlank()
        && !street.isBlank()
        && !number.isBlank()
        && !district.isBlank()
        && !city.isBlank()
        && !state.isBlank();
  }

  private void validateBusinessRules() {
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

  private String normalize(String value) {
    return value.trim();
  }
}
