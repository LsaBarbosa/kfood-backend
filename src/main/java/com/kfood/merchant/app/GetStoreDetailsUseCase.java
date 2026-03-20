package com.kfood.merchant.app;

import com.kfood.merchant.api.StoreDetailsResponse;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CurrentTenantProvider.class,
  StoreActivationRequirementsService.class
})
public class GetStoreDetailsUseCase {

  private final StoreRepository storeRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreActivationRequirementsService storeActivationRequirementsService;

  public GetStoreDetailsUseCase(
      StoreRepository storeRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreActivationRequirementsService storeActivationRequirementsService) {
    this.storeRepository = storeRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeActivationRequirementsService = storeActivationRequirementsService;
  }

  @Transactional(readOnly = true)
  public StoreDetailsResponse execute() {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    var requirements = storeActivationRequirementsService.evaluate(storeId);
    return StoreDetailsMapper.toResponse(store, requirements);
  }
}
