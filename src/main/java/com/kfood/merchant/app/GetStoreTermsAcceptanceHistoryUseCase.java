package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  StoreTermsAcceptanceRepository.class,
  CurrentTenantProvider.class
})
public class GetStoreTermsAcceptanceHistoryUseCase {

  private final StoreRepository storeRepository;
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository;
  private final CurrentTenantProvider currentTenantProvider;

  public GetStoreTermsAcceptanceHistoryUseCase(
      StoreRepository storeRepository,
      StoreTermsAcceptanceRepository storeTermsAcceptanceRepository,
      CurrentTenantProvider currentTenantProvider) {
    this.storeRepository = storeRepository;
    this.storeTermsAcceptanceRepository = storeTermsAcceptanceRepository;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public List<StoreTermsAcceptanceHistoryItemOutput> execute() {
    var storeId = currentTenantProvider.getRequiredStoreId();
    storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    return storeTermsAcceptanceRepository.findAllByStoreIdOrderByAcceptedAtDesc(storeId).stream()
        .map(StoreTermsAcceptanceMapper::toHistoryItemOutput)
        .toList();
  }
}
