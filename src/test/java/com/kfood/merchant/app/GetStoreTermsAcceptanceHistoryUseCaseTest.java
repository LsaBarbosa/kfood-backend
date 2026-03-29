package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStoreTermsAcceptanceHistoryUseCaseTest {

  private final MerchantQueryPort merchantQueryPort = mock(MerchantQueryPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final GetStoreTermsAcceptanceHistoryUseCase getStoreTermsAcceptanceHistoryUseCase =
      new GetStoreTermsAcceptanceHistoryUseCase(merchantQueryPort, currentTenantProvider);

  @Test
  void shouldReturnAcceptanceHistoryOrderedByMostRecentFirst() {
    var storeId = UUID.randomUUID();
    var output =
        List.of(
            new StoreTermsAcceptanceHistoryItemOutput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LegalDocumentType.TERMS_OF_USE,
                "2026.04",
                Instant.parse("2026-04-20T13:15:00Z")),
            new StoreTermsAcceptanceHistoryItemOutput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LegalDocumentType.TERMS_OF_USE,
                "2026.03",
                Instant.parse("2026-03-20T13:15:00Z")));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantQueryPort.getStoreTermsAcceptanceHistory(storeId)).thenReturn(output);

    var history = getStoreTermsAcceptanceHistoryUseCase.execute();

    assertThat(history).hasSize(2);
    assertThat(history.getFirst().documentVersion()).isEqualTo("2026.04");
    assertThat(history.getLast().documentVersion()).isEqualTo("2026.03");
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantQueryPort.getStoreTermsAcceptanceHistory(storeId))
        .thenThrow(new StoreNotFoundException(storeId));

    assertThatThrownBy(() -> getStoreTermsAcceptanceHistoryUseCase.execute())
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }
}
