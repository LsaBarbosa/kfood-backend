package com.kfood.merchant.infra.adapter;

import com.kfood.merchant.app.StoreActivationRequirements;
import com.kfood.merchant.app.port.MerchantActivationRequirementsPort;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaMerchantActivationRequirementsAdapter
    implements MerchantActivationRequirementsPort {

  private final StoreBusinessHourRepository storeBusinessHourRepository;
  private final DeliveryZoneRepository deliveryZoneRepository;
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository;
  private final StoreRepository storeRepository;

  public JpaMerchantActivationRequirementsAdapter(
      StoreBusinessHourRepository storeBusinessHourRepository,
      DeliveryZoneRepository deliveryZoneRepository,
      StoreTermsAcceptanceRepository storeTermsAcceptanceRepository,
      StoreRepository storeRepository) {
    this.storeBusinessHourRepository = storeBusinessHourRepository;
    this.deliveryZoneRepository = deliveryZoneRepository;
    this.storeTermsAcceptanceRepository = storeTermsAcceptanceRepository;
    this.storeRepository = storeRepository;
  }

  @Override
  public StoreActivationRequirements evaluate(UUID storeId) {
    var store = storeRepository.findById(storeId).orElse(null);
    var hoursConfigured = storeBusinessHourRepository.existsByStoreId(storeId);
    var deliveryZonesConfigured = deliveryZoneRepository.existsByStoreIdAndActiveTrue(storeId);
    var termsAccepted =
        storeTermsAcceptanceRepository.existsByStoreIdAndDocumentType(
            storeId, LegalDocumentType.TERMS_OF_USE);
    return new StoreActivationRequirements(
        store != null && store.hasCategoryConfigured(),
        store != null && store.hasAddressConfigured(),
        hoursConfigured,
        deliveryZonesConfigured,
        termsAccepted);
  }
}
