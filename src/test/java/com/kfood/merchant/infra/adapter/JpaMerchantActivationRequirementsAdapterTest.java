package com.kfood.merchant.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaMerchantActivationRequirementsAdapterTest {

  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository =
      mock(StoreTermsAcceptanceRepository.class);
  private final JpaMerchantActivationRequirementsAdapter adapter =
      new JpaMerchantActivationRequirementsAdapter(
          storeBusinessHourRepository, deliveryZoneRepository, storeTermsAcceptanceRepository);

  @Test
  void shouldEvaluateActivationRequirements() {
    var storeId = UUID.randomUUID();

    when(storeBusinessHourRepository.existsByStoreId(storeId)).thenReturn(true);
    when(deliveryZoneRepository.existsByStoreIdAndActiveTrue(storeId)).thenReturn(false);
    when(storeTermsAcceptanceRepository.existsByStoreIdAndDocumentType(
            storeId, LegalDocumentType.TERMS_OF_USE))
        .thenReturn(true);

    var result = adapter.evaluate(storeId);

    assertThat(result.hoursConfigured()).isTrue();
    assertThat(result.deliveryZonesConfigured()).isFalse();
    assertThat(result.termsAccepted()).isTrue();
  }
}
