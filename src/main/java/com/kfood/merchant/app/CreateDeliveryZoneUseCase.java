package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({MerchantCommandPort.class, CurrentTenantProvider.class})
public class CreateDeliveryZoneUseCase {

  private final MerchantCommandPort merchantCommandPort;
  private final CurrentTenantProvider currentTenantProvider;

  public CreateDeliveryZoneUseCase(
      MerchantCommandPort merchantCommandPort, CurrentTenantProvider currentTenantProvider) {
    this.merchantCommandPort = merchantCommandPort;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional
  public DeliveryZoneOutput execute(CreateDeliveryZoneCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    return merchantCommandPort.createDeliveryZone(storeId, command);
  }
}
