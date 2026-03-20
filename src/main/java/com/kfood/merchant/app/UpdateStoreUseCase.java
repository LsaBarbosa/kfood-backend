package com.kfood.merchant.app;

import com.kfood.merchant.api.StoreResponse;
import com.kfood.merchant.api.UpdateStoreRequest;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({StoreRepository.class, CurrentTenantProvider.class})
public class UpdateStoreUseCase {

  private final StoreRepository storeRepository;
  private final CurrentTenantProvider currentTenantProvider;

  public UpdateStoreUseCase(
      StoreRepository storeRepository, CurrentTenantProvider currentTenantProvider) {
    this.storeRepository = storeRepository;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional
  public StoreResponse execute(UpdateStoreRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    if (request.slug() != null
        && !request.slug().equals(store.getSlug())
        && storeRepository.existsBySlugAndIdNot(request.slug(), storeId)) {
      throw new StoreSlugAlreadyExistsException(request.slug());
    }

    if (request.name() != null) {
      store.changeName(request.name());
    }
    if (request.slug() != null) {
      store.changeSlug(request.slug());
    }
    if (request.cnpj() != null) {
      store.changeCnpj(request.cnpj());
    }
    if (request.phone() != null) {
      store.changePhone(request.phone());
    }
    if (request.timezone() != null) {
      store.changeTimezone(request.timezone());
    }

    var savedStore = storeRepository.saveAndFlush(store);
    return StoreMapper.toResponse(savedStore);
  }
}
