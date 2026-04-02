package com.kfood.merchant.app;

import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class MerchantViews {

  private MerchantViews() {}

  public record StoreView(
      UUID id,
      String name,
      String slug,
      String cnpj,
      String phone,
      String timezone,
      StoreCategory category,
      StoreAddressView address,
      StoreStatus status,
      Instant createdAt) {

    public StoreView(
        UUID id,
        String name,
        String slug,
        String cnpj,
        String phone,
        String timezone,
        StoreStatus status,
        Instant createdAt) {
      this(id, name, slug, cnpj, phone, timezone, null, null, status, createdAt);
    }
  }

  public record StoreAddressView(
      String zipCode, String street, String number, String district, String city, String state) {}

  public record DeliveryZoneView(
      UUID id, String zoneName, BigDecimal feeAmount, BigDecimal minOrderAmount, boolean active) {}

  public record StoreHourView(
      DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {}

  public record StoreTermsAcceptanceView(
      UUID id,
      UUID acceptedByUserId,
      LegalDocumentType documentType,
      String documentVersion,
      Instant acceptedAt) {}

  public record PublicStoreMenuProductView(
      UUID id,
      String name,
      String description,
      BigDecimal basePrice,
      String imageUrl,
      boolean paused,
      List<PublicStoreMenuOptionGroupView> optionGroups) {}

  public record PublicStoreMenuOptionGroupView(
      UUID id,
      String name,
      Integer minSelect,
      Integer maxSelect,
      boolean required,
      List<PublicStoreMenuOptionItemView> items) {}

  public record PublicStoreMenuOptionItemView(
      UUID id, String name, BigDecimal extraPrice, Integer sortOrder) {}
}
