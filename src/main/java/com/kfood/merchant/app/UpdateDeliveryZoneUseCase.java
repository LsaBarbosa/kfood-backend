package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({MerchantCommandPort.class, CurrentTenantProvider.class})
public class UpdateDeliveryZoneUseCase {

  private final MerchantCommandPort merchantCommandPort;
  private final CurrentTenantProvider currentTenantProvider;

  public UpdateDeliveryZoneUseCase(
      MerchantCommandPort merchantCommandPort, CurrentTenantProvider currentTenantProvider) {
    this.merchantCommandPort = merchantCommandPort;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional
  public DeliveryZoneOutput execute(UUID zoneId, UpdateDeliveryZoneCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    return merchantCommandPort.updateDeliveryZone(storeId, zoneId, command);
  }
}
