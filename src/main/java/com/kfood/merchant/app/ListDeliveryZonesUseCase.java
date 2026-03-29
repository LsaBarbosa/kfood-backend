package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({DeliveryZoneRepository.class, CurrentTenantProvider.class})
public class ListDeliveryZonesUseCase {

  private final DeliveryZoneRepository deliveryZoneRepository;
  private final CurrentTenantProvider currentTenantProvider;

  public ListDeliveryZonesUseCase(
      DeliveryZoneRepository deliveryZoneRepository, CurrentTenantProvider currentTenantProvider) {
    this.deliveryZoneRepository = deliveryZoneRepository;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public List<DeliveryZoneOutput> execute() {
    var storeId = currentTenantProvider.getRequiredStoreId();
    return deliveryZoneRepository.findAllByStoreIdOrderByZoneNameAsc(storeId).stream()
        .map(DeliveryZoneMapper::toOutput)
        .toList();
  }
}
