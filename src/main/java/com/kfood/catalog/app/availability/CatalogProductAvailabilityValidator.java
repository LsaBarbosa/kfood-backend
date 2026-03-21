package com.kfood.catalog.app.availability;

import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CatalogProductAvailabilityValidator {

  private final CatalogProductAvailabilityEvaluator catalogProductAvailabilityEvaluator;

  public CatalogProductAvailabilityValidator(
      CatalogProductAvailabilityEvaluator catalogProductAvailabilityEvaluator) {
    this.catalogProductAvailabilityEvaluator = catalogProductAvailabilityEvaluator;
  }

  public void ensureAvailableNow(CatalogProduct product, String storeTimezone) {
    if (!catalogProductAvailabilityEvaluator.isAvailableNow(product, ZoneId.of(storeTimezone))) {
      throw new BusinessException(
          ErrorCode.CATALOG_ITEM_UNAVAILABLE,
          "Catalog product is not available in the current time window",
          HttpStatus.UNPROCESSABLE_CONTENT);
    }
  }
}
