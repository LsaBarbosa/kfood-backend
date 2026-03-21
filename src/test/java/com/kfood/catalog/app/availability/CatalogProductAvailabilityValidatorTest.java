package com.kfood.catalog.app.availability;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.junit.jupiter.api.Test;

class CatalogProductAvailabilityValidatorTest {

  private final CatalogProductAvailabilityEvaluator catalogProductAvailabilityEvaluator =
      mock(CatalogProductAvailabilityEvaluator.class);
  private final CatalogProductAvailabilityValidator catalogProductAvailabilityValidator =
      new CatalogProductAvailabilityValidator(catalogProductAvailabilityEvaluator);

  @Test
  void shouldThrowCatalogItemUnavailableWhenProductIsOutsideWindow() {
    var product = mock(CatalogProduct.class);
    when(catalogProductAvailabilityEvaluator.isAvailableNow(
            product, java.time.ZoneId.of("America/Sao_Paulo")))
        .thenReturn(false);

    assertThatThrownBy(
            () ->
                catalogProductAvailabilityValidator.ensureAvailableNow(
                    product, "America/Sao_Paulo"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CATALOG_ITEM_UNAVAILABLE);
  }
}
