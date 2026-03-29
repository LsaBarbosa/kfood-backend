package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CurrentTenantProvider.class,
  StoreOperationalGuard.class
})
public class UpdateStoreUseCase {

  private final StoreRepository storeRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public UpdateStoreUseCase(
      StoreRepository storeRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreOperationalGuard storeOperationalGuard) {
    this.storeRepository = storeRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeOperationalGuard = storeOperationalGuard;
  }

  @Transactional
  public StoreOutput execute(UpdateStoreCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    if (command.slug() != null
        && !command.slug().equals(store.getSlug())
        && storeRepository.existsBySlugAndIdNot(command.slug(), storeId)) {
      throw new StoreSlugAlreadyExistsException(command.slug());
    }

    if (command.name() != null) {
      store.changeName(command.name());
    }
    if (command.slug() != null) {
      store.changeSlug(command.slug());
    }
    if (command.cnpj() != null) {
      store.changeCnpj(command.cnpj());
    }
    if (command.phone() != null) {
      store.changePhone(command.phone());
    }
    if (command.timezone() != null) {
      store.changeTimezone(command.timezone());
    }

    var savedStore = storeRepository.saveAndFlush(store);
    return new StoreOutput(
        savedStore.getId(),
        savedStore.getName(),
        savedStore.getSlug(),
        savedStore.getCnpj(),
        savedStore.getPhone(),
        savedStore.getTimezone(),
        savedStore.getStatus());
  }
}
