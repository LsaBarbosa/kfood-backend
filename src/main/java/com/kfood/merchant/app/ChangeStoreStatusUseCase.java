package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  MerchantCommandPort.class,
  CurrentTenantProvider.class,
  StoreActivationRequirementsService.class
})
public class ChangeStoreStatusUseCase {

  private final MerchantCommandPort merchantCommandPort;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreActivationRequirementsService storeActivationRequirementsService;

  public ChangeStoreStatusUseCase(
      MerchantCommandPort merchantCommandPort,
      CurrentTenantProvider currentTenantProvider,
      StoreActivationRequirementsService storeActivationRequirementsService) {
    this.merchantCommandPort = merchantCommandPort;
    this.currentTenantProvider = currentTenantProvider;
    this.storeActivationRequirementsService = storeActivationRequirementsService;
  }

  @Transactional
  public StoreDetailsOutput execute(ChangeStoreStatusCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var requirements = storeActivationRequirementsService.evaluate(storeId);
    return merchantCommandPort.changeStoreStatus(storeId, command, requirements);
  }
}
