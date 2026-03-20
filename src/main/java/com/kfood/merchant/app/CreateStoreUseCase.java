package com.kfood.merchant.app;

import com.kfood.merchant.api.CreateStoreRequest;
import com.kfood.merchant.api.CreateStoreResponse;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(StoreRepository.class)
public class CreateStoreUseCase {

  private final StoreRepository storeRepository;

  public CreateStoreUseCase(StoreRepository storeRepository) {
    this.storeRepository = storeRepository;
  }

  @Transactional
  public CreateStoreResponse execute(CreateStoreRequest request) {
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
    return StoreMapper.toCreateResponse(savedStore);
  }
}
