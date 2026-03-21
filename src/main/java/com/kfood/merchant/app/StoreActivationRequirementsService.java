package com.kfood.merchant.app;

import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({
  StoreBusinessHourRepository.class,
  DeliveryZoneRepository.class,
  StoreTermsAcceptanceRepository.class
})
public class StoreActivationRequirementsService {

  private final StoreBusinessHourRepository storeBusinessHourRepository;
  private final DeliveryZoneRepository deliveryZoneRepository;
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository;

  public StoreActivationRequirementsService(
      StoreBusinessHourRepository storeBusinessHourRepository,
      DeliveryZoneRepository deliveryZoneRepository,
      StoreTermsAcceptanceRepository storeTermsAcceptanceRepository) {
    this.storeBusinessHourRepository = storeBusinessHourRepository;
    this.deliveryZoneRepository = deliveryZoneRepository;
    this.storeTermsAcceptanceRepository = storeTermsAcceptanceRepository;
  }

  public StoreActivationRequirements evaluate(java.util.UUID storeId) {
    var hoursConfigured = storeBusinessHourRepository.existsByStoreId(storeId);
    var deliveryZonesConfigured = deliveryZoneRepository.existsByStoreIdAndActiveTrue(storeId);
    var termsAccepted =
        storeTermsAcceptanceRepository.existsByStoreIdAndDocumentType(
            storeId, LegalDocumentType.TERMS_OF_USE);
    return new StoreActivationRequirements(hoursConfigured, deliveryZonesConfigured, termsAccepted);
  }
}
