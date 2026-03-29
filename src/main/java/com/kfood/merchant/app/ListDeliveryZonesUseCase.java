package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({MerchantQueryPort.class, CurrentTenantProvider.class})
public class ListDeliveryZonesUseCase {

  private final MerchantQueryPort merchantQueryPort;
  private final CurrentTenantProvider currentTenantProvider;

  public ListDeliveryZonesUseCase(
      MerchantQueryPort merchantQueryPort, CurrentTenantProvider currentTenantProvider) {
    this.merchantQueryPort = merchantQueryPort;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public List<DeliveryZoneOutput> execute() {
    var storeId = currentTenantProvider.getRequiredStoreId();
    return merchantQueryPort.listDeliveryZones(storeId);
  }
}
