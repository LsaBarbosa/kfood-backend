package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreActivationRequirementsServiceTest {

  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository =
      mock(StoreTermsAcceptanceRepository.class);
  private final StoreActivationRequirementsService storeActivationRequirementsService =
      new StoreActivationRequirementsService(
          storeBusinessHourRepository, deliveryZoneRepository, storeTermsAcceptanceRepository);

  @Test
  void shouldEvaluateTermsOfUseAsActivationRequirement() {
    var storeId = UUID.randomUUID();

    when(storeBusinessHourRepository.existsByStoreId(storeId)).thenReturn(true);
    when(deliveryZoneRepository.existsByStoreIdAndActiveTrue(storeId)).thenReturn(true);
    when(storeTermsAcceptanceRepository.existsByStoreIdAndDocumentType(
            storeId, LegalDocumentType.TERMS_OF_USE))
        .thenReturn(false);

    var requirements = storeActivationRequirementsService.evaluate(storeId);

    assertThat(requirements.hoursConfigured()).isTrue();
    assertThat(requirements.deliveryZonesConfigured()).isTrue();
    assertThat(requirements.termsAccepted()).isFalse();
    assertThat(requirements.canActivate()).isFalse();
    assertThat(requirements.missingRequirements()).containsExactly("termsAccepted");

    verify(storeTermsAcceptanceRepository)
        .existsByStoreIdAndDocumentType(storeId, LegalDocumentType.TERMS_OF_USE);
  }
}
