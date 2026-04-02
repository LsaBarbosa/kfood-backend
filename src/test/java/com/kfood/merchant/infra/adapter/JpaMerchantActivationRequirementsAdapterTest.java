package com.kfood.merchant.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreAddress;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaMerchantActivationRequirementsAdapterTest {

  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository =
      mock(StoreTermsAcceptanceRepository.class);
  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final JpaMerchantActivationRequirementsAdapter adapter =
      new JpaMerchantActivationRequirementsAdapter(
          storeBusinessHourRepository,
          deliveryZoneRepository,
          storeTermsAcceptanceRepository,
          storeRepository);

  @Test
  void shouldEvaluateActivationRequirements() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo",
            StoreCategory.PIZZARIA,
            new StoreAddress("25000-000", "Rua Central", "100", "Centro", "Mage", "RJ"));

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeBusinessHourRepository.existsByStoreId(storeId)).thenReturn(true);
    when(deliveryZoneRepository.existsByStoreIdAndActiveTrue(storeId)).thenReturn(false);
    when(storeTermsAcceptanceRepository.existsByStoreIdAndDocumentType(
            storeId, LegalDocumentType.TERMS_OF_USE))
        .thenReturn(true);

    var result = adapter.evaluate(storeId);

    assertThat(result.categoryConfigured()).isTrue();
    assertThat(result.addressConfigured()).isTrue();
    assertThat(result.hoursConfigured()).isTrue();
    assertThat(result.deliveryZonesConfigured()).isFalse();
    assertThat(result.termsAccepted()).isTrue();
  }

  @Test
  void shouldReturnCategoryAndAddressAsNotConfiguredWhenStoreIsMissing() {
    var storeId = UUID.randomUUID();

    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());
    when(storeBusinessHourRepository.existsByStoreId(storeId)).thenReturn(false);
    when(deliveryZoneRepository.existsByStoreIdAndActiveTrue(storeId)).thenReturn(false);
    when(storeTermsAcceptanceRepository.existsByStoreIdAndDocumentType(
            storeId, LegalDocumentType.TERMS_OF_USE))
        .thenReturn(false);

    var result = adapter.evaluate(storeId);

    assertThat(result.categoryConfigured()).isFalse();
    assertThat(result.addressConfigured()).isFalse();
    assertThat(result.hoursConfigured()).isFalse();
    assertThat(result.deliveryZonesConfigured()).isFalse();
    assertThat(result.termsAccepted()).isFalse();
  }
}
