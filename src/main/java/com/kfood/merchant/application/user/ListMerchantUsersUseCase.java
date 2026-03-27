package com.kfood.merchant.application.user;

import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class ListMerchantUsersUseCase {

  private final MerchantUserManagementPort merchantUserManagementPort;
  private final MerchantTenantAccessPort merchantTenantAccessPort;

  public ListMerchantUsersUseCase(
      MerchantUserManagementPort merchantUserManagementPort,
      MerchantTenantAccessPort merchantTenantAccessPort) {
    this.merchantUserManagementPort = merchantUserManagementPort;
    this.merchantTenantAccessPort = merchantTenantAccessPort;
  }

  @Transactional(readOnly = true)
  public List<MerchantUserOutput> execute() {
    return merchantUserManagementPort.listByStoreId(merchantTenantAccessPort.getRequiredStoreId());
  }
}
