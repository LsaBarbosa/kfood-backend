package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreActivationRequirementsServiceTest {

  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final StoreActivationRequirementsService storeActivationRequirementsService =
      new StoreActivationRequirementsService(storeBusinessHourRepository, deliveryZoneRepository);

  @Test
  void shouldEvaluateRequirements() {
    var storeId = UUID.randomUUID();

    when(storeBusinessHourRepository.existsByStoreId(storeId)).thenReturn(true);
    when(deliveryZoneRepository.existsByStoreIdAndActiveTrue(storeId)).thenReturn(false);

    var requirements = storeActivationRequirementsService.evaluate(storeId);

    assertThat(requirements.hoursConfigured()).isTrue();
    assertThat(requirements.deliveryZonesConfigured()).isFalse();
    assertThat(requirements.canActivate()).isFalse();
    assertThat(requirements.missingRequirements()).containsExactly("deliveryZonesConfigured");
  }
}
