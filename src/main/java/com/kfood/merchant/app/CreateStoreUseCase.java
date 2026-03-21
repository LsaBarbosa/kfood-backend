package com.kfood.merchant.app;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.api.CreateStoreRequest;
import com.kfood.merchant.api.CreateStoreResponse;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  IdentityUserRepository.class,
  CurrentAuthenticatedUserProvider.class
})
public class CreateStoreUseCase {

  private final StoreRepository storeRepository;
  private final IdentityUserRepository identityUserRepository;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;

  public CreateStoreUseCase(
      StoreRepository storeRepository,
      IdentityUserRepository identityUserRepository,
      CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider) {
    this.storeRepository = storeRepository;
    this.identityUserRepository = identityUserRepository;
    this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
  }

  @Transactional
  public CreateStoreResponse execute(CreateStoreRequest request) {
    var authenticatedUserId = currentAuthenticatedUserProvider.getRequiredUserId();
    var authenticatedUser =
        identityUserRepository
            .findDetailedById(authenticatedUserId)
            .orElseThrow(() -> new AuthenticatedUserNotFoundException(authenticatedUserId));

    if (authenticatedUser.hasRole(UserRoleName.OWNER) && authenticatedUser.getStoreId() != null) {
      throw new OwnerAlreadyBoundToAnotherStoreException(authenticatedUser.getStoreId());
    }

    if (storeRepository.existsBySlug(request.slug())) {
      throw new StoreSlugAlreadyExistsException(request.slug());
    }

    var store =
        new Store(
            UUID.randomUUID(),
            request.name(),
            request.slug(),
            request.cnpj(),
            request.phone(),
            request.timezone());

    var savedStore = storeRepository.saveAndFlush(store);

    if (authenticatedUser.hasRole(UserRoleName.OWNER)) {
      authenticatedUser.bindToStore(savedStore.getId());
      identityUserRepository.saveAndFlush(authenticatedUser);
    }

    return StoreMapper.toCreateResponse(savedStore);
  }
}
