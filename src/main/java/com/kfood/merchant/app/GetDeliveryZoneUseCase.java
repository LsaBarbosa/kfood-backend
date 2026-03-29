package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({MerchantQueryPort.class, CurrentTenantProvider.class})
public class GetDeliveryZoneUseCase {

  private final MerchantQueryPort merchantQueryPort;
  private final CurrentTenantProvider currentTenantProvider;

  public GetDeliveryZoneUseCase(
      MerchantQueryPort merchantQueryPort, CurrentTenantProvider currentTenantProvider) {
    this.merchantQueryPort = merchantQueryPort;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public DeliveryZoneOutput execute(UUID zoneId) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    return merchantQueryPort.getDeliveryZone(storeId, zoneId);
  }
}
