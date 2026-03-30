package com.kfood.merchant.app.port;

import com.kfood.merchant.app.StoreActivationRequirements;
import java.util.UUID;

public interface MerchantActivationRequirementsPort {

  StoreActivationRequirements evaluate(UUID storeId);
}
