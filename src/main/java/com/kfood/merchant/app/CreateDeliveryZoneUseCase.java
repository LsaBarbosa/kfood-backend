package com.kfood.merchant.app;

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
  CurrentTenantProvider.class,
  StoreOperationalGuard.class
})
public class CreateDeliveryZoneUseCase {

  private final StoreRepository storeRepository;
  private final DeliveryZoneRepository deliveryZoneRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public CreateDeliveryZoneUseCase(
      StoreRepository storeRepository,
      DeliveryZoneRepository deliveryZoneRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreOperationalGuard storeOperationalGuard) {
    this.storeRepository = storeRepository;
    this.deliveryZoneRepository = deliveryZoneRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeOperationalGuard = storeOperationalGuard;
  }

  @Transactional
  public DeliveryZoneOutput execute(CreateDeliveryZoneCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    var zoneName = command.zoneName().trim();

    if (deliveryZoneRepository.existsByStoreIdAndZoneName(storeId, zoneName)) {
      throw new DeliveryZoneAlreadyExistsException(zoneName);
    }

    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            zoneName,
            command.feeAmount(),
            command.minOrderAmount(),
            command.active());

    return DeliveryZoneMapper.toOutput(deliveryZoneRepository.saveAndFlush(zone));
  }
}
