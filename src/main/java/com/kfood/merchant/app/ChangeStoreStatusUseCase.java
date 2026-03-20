package com.kfood.merchant.app;

import com.kfood.merchant.api.ChangeStoreStatusRequest;
import com.kfood.merchant.api.StoreDetailsResponse;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CurrentTenantProvider.class,
  StoreActivationRequirementsService.class
})
public class ChangeStoreStatusUseCase {

  private final StoreRepository storeRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreActivationRequirementsService storeActivationRequirementsService;

  public ChangeStoreStatusUseCase(
      StoreRepository storeRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreActivationRequirementsService storeActivationRequirementsService) {
    this.storeRepository = storeRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeActivationRequirementsService = storeActivationRequirementsService;
  }

  @Transactional
  public StoreDetailsResponse execute(ChangeStoreStatusRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    var requirements = storeActivationRequirementsService.evaluate(storeId);

    if (request.targetStatus() == StoreStatus.ACTIVE) {
      if (!requirements.canActivate()) {
        throw new StoreActivationRequirementsNotMetException(requirements.missingRequirements());
      }
      store.activate();
    } else if (request.targetStatus() == StoreStatus.SUSPENDED) {
      store.suspend();
    } else {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Changing status to SETUP is not allowed",
          HttpStatus.BAD_REQUEST);
    }

    var savedStore = storeRepository.saveAndFlush(store);
    return StoreDetailsMapper.toResponse(savedStore, requirements);
  }
}
