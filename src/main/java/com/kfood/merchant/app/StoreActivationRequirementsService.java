package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({StoreBusinessHourRepository.class, DeliveryZoneRepository.class})
public class StoreActivationRequirementsService {

  private final StoreBusinessHourRepository storeBusinessHourRepository;
  private final DeliveryZoneRepository deliveryZoneRepository;

  public StoreActivationRequirementsService(
      StoreBusinessHourRepository storeBusinessHourRepository,
      DeliveryZoneRepository deliveryZoneRepository) {
    this.storeBusinessHourRepository = storeBusinessHourRepository;
    this.deliveryZoneRepository = deliveryZoneRepository;
  }

  public StoreActivationRequirements evaluate(java.util.UUID storeId) {
    var hoursConfigured = storeBusinessHourRepository.existsByStoreId(storeId);
    var deliveryZonesConfigured = deliveryZoneRepository.existsByStoreIdAndActiveTrue(storeId);
    return new StoreActivationRequirements(hoursConfigured, deliveryZonesConfigured);
  }
}
