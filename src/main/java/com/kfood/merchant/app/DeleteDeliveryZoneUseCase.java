package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({MerchantCommandPort.class, CurrentTenantProvider.class})
public class DeleteDeliveryZoneUseCase {

  private final MerchantCommandPort merchantCommandPort;
  private final CurrentTenantProvider currentTenantProvider;

  public DeleteDeliveryZoneUseCase(
      MerchantCommandPort merchantCommandPort, CurrentTenantProvider currentTenantProvider) {
    this.merchantCommandPort = merchantCommandPort;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional
  public void execute(UUID zoneId) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    merchantCommandPort.deleteDeliveryZone(storeId, zoneId);
  }
}
