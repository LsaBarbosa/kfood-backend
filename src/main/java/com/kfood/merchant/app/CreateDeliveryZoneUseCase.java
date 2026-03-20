package com.kfood.merchant.app;

import com.kfood.merchant.api.CreateDeliveryZoneRequest;
import com.kfood.merchant.api.DeliveryZoneResponse;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  DeliveryZoneRepository.class,
  CurrentTenantProvider.class
})
public class CreateDeliveryZoneUseCase {

  private final StoreRepository storeRepository;
  private final DeliveryZoneRepository deliveryZoneRepository;
  private final CurrentTenantProvider currentTenantProvider;

  public CreateDeliveryZoneUseCase(
      StoreRepository storeRepository,
      DeliveryZoneRepository deliveryZoneRepository,
      CurrentTenantProvider currentTenantProvider) {
    this.storeRepository = storeRepository;
    this.deliveryZoneRepository = deliveryZoneRepository;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional
  public DeliveryZoneResponse execute(CreateDeliveryZoneRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    var zoneName = request.zoneName().trim();

    if (deliveryZoneRepository.existsByStoreIdAndZoneName(storeId, zoneName)) {
      throw new DeliveryZoneAlreadyExistsException(zoneName);
    }

    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            zoneName,
            request.feeAmount(),
            request.minOrderAmount(),
            request.active());

    return DeliveryZoneMapper.toResponse(deliveryZoneRepository.saveAndFlush(zone));
  }
}
