package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  MerchantQueryPort.class,
  CurrentTenantProvider.class,
  StoreActivationRequirementsService.class
})
public class GetStoreDetailsUseCase {

  private final MerchantQueryPort merchantQueryPort;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreActivationRequirementsService storeActivationRequirementsService;

  public GetStoreDetailsUseCase(
      MerchantQueryPort merchantQueryPort,
      CurrentTenantProvider currentTenantProvider,
      StoreActivationRequirementsService storeActivationRequirementsService) {
    this.merchantQueryPort = merchantQueryPort;
    this.currentTenantProvider = currentTenantProvider;
    this.storeActivationRequirementsService = storeActivationRequirementsService;
  }

  @Transactional(readOnly = true)
  public StoreDetailsOutput execute() {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var requirements = storeActivationRequirementsService.evaluate(storeId);
    return merchantQueryPort.getStoreDetails(storeId, requirements);
  }
}
