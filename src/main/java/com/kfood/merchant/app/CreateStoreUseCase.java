package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({MerchantCommandPort.class, CurrentAuthenticatedUserProvider.class})
public class CreateStoreUseCase {

  private final MerchantCommandPort merchantCommandPort;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;

  public CreateStoreUseCase(
      MerchantCommandPort merchantCommandPort,
      CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider) {
    this.merchantCommandPort = merchantCommandPort;
    this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
  }

  @Transactional
  public CreateStoreOutput execute(CreateStoreCommand command) {
    var authenticatedUserId = currentAuthenticatedUserProvider.getRequiredUserId();
    return merchantCommandPort.createStore(authenticatedUserId, command);
  }
}
