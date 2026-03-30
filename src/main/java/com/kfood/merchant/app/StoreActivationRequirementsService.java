package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantActivationRequirementsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(MerchantActivationRequirementsPort.class)
public class StoreActivationRequirementsService {

  private final MerchantActivationRequirementsPort merchantActivationRequirementsPort;

  public StoreActivationRequirementsService(
      MerchantActivationRequirementsPort merchantActivationRequirementsPort) {
    this.merchantActivationRequirementsPort = merchantActivationRequirementsPort;
  }

  public StoreActivationRequirements evaluate(java.util.UUID storeId) {
    return merchantActivationRequirementsPort.evaluate(storeId);
  }
}
