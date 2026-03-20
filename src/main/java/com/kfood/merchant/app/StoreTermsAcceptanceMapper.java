package com.kfood.merchant.app;

import com.kfood.merchant.api.StoreTermsAcceptanceHistoryItemResponse;
import com.kfood.merchant.api.StoreTermsAcceptanceResponse;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptance;

public final class StoreTermsAcceptanceMapper {

  private StoreTermsAcceptanceMapper() {}

  public static StoreTermsAcceptanceResponse toResponse(StoreTermsAcceptance acceptance) {
    return new StoreTermsAcceptanceResponse(
        acceptance.getId(),
        acceptance.getDocumentType(),
        acceptance.getDocumentVersion(),
        acceptance.getAcceptedAt());
  }

  public static StoreTermsAcceptanceHistoryItemResponse toHistoryItem(
      StoreTermsAcceptance acceptance) {
    return new StoreTermsAcceptanceHistoryItemResponse(
        acceptance.getId(),
        acceptance.getAcceptedByUserId(),
        acceptance.getDocumentType(),
        acceptance.getDocumentVersion(),
        acceptance.getAcceptedAt());
  }
}
