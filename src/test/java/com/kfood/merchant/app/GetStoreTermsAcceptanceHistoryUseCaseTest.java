package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptance;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStoreTermsAcceptanceHistoryUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository =
      mock(StoreTermsAcceptanceRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final GetStoreTermsAcceptanceHistoryUseCase getStoreTermsAcceptanceHistoryUseCase =
      new GetStoreTermsAcceptanceHistoryUseCase(
          storeRepository, storeTermsAcceptanceRepository, currentTenantProvider);

  @Test
  void shouldReturnAcceptanceHistoryOrderedByMostRecentFirst() {
    var storeId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store(storeId)));
    when(storeTermsAcceptanceRepository.findAllByStoreIdOrderByAcceptedAtDesc(storeId))
        .thenReturn(
            List.of(
                acceptance(storeId, "2026.04", Instant.parse("2026-04-20T13:15:00Z")),
                acceptance(storeId, "2026.03", Instant.parse("2026-03-20T13:15:00Z"))));

    var history = getStoreTermsAcceptanceHistoryUseCase.execute();

    assertThat(history).hasSize(2);
    assertThat(history.getFirst().documentVersion()).isEqualTo("2026.04");
    assertThat(history.getLast().documentVersion()).isEqualTo("2026.03");
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getStoreTermsAcceptanceHistoryUseCase.execute())
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  private Store store(UUID storeId) {
    return new Store(
        storeId,
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }

  private StoreTermsAcceptance acceptance(UUID storeId, String version, Instant acceptedAt) {
    return new StoreTermsAcceptance(
        UUID.randomUUID(),
        storeId,
        UUID.randomUUID(),
        LegalDocumentType.TERMS_OF_USE,
        version,
        acceptedAt,
        "203.0.113.9");
  }
}
