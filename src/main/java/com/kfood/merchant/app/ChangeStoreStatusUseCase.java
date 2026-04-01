package com.kfood.merchant.app;

import com.kfood.merchant.app.audit.MerchantStoreAuditPort;
import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  MerchantCommandPort.class,
  MerchantQueryPort.class,
  MerchantStoreAuditPort.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class,
  StoreActivationRequirementsService.class
})
public class ChangeStoreStatusUseCase {

  private final MerchantCommandPort merchantCommandPort;
  private final MerchantQueryPort merchantQueryPort;
  private final MerchantStoreAuditPort merchantStoreAuditPort;
  private final CurrentTenantProvider currentTenantProvider;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;
  private final StoreActivationRequirementsService storeActivationRequirementsService;

  public ChangeStoreStatusUseCase(
      MerchantCommandPort merchantCommandPort,
      MerchantQueryPort merchantQueryPort,
      MerchantStoreAuditPort merchantStoreAuditPort,
      CurrentTenantProvider currentTenantProvider,
      CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider,
      StoreActivationRequirementsService storeActivationRequirementsService) {
    this.merchantCommandPort = merchantCommandPort;
    this.merchantQueryPort = merchantQueryPort;
    this.merchantStoreAuditPort = merchantStoreAuditPort;
    this.currentTenantProvider = currentTenantProvider;
    this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
    this.storeActivationRequirementsService = storeActivationRequirementsService;
  }

  @Transactional
  public StoreDetailsOutput execute(ChangeStoreStatusCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var authenticatedUserId = currentAuthenticatedUserProvider.getRequiredUserId();
    var requirements = storeActivationRequirementsService.evaluate(storeId);
    var previousStatus = merchantQueryPort.getStoreDetails(storeId, requirements).status();
    var result = merchantCommandPort.changeStoreStatus(storeId, command, requirements);
    merchantStoreAuditPort.recordStoreStatusChanged(
        storeId, authenticatedUserId, previousStatus, result.status());
    return result;
  }
}
