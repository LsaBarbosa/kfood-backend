package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({DeliveryZoneRepository.class, CurrentTenantProvider.class})
public class GetDeliveryZoneUseCase {

  private final DeliveryZoneRepository deliveryZoneRepository;
  private final CurrentTenantProvider currentTenantProvider;

  public GetDeliveryZoneUseCase(
      DeliveryZoneRepository deliveryZoneRepository, CurrentTenantProvider currentTenantProvider) {
    this.deliveryZoneRepository = deliveryZoneRepository;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public DeliveryZoneOutput execute(UUID zoneId) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var zone =
        deliveryZoneRepository
            .findByIdAndStoreId(zoneId, storeId)
            .orElseThrow(() -> new DeliveryZoneNotFoundException(zoneId));
    return DeliveryZoneMapper.toOutput(zone);
  }
}
