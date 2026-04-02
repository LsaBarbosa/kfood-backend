package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.audit.MerchantStoreAuditPort;
import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class CreateStoreTermsAcceptanceUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final MerchantStoreAuditPort merchantStoreAuditPort = mock(MerchantStoreAuditPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final CreateStoreTermsAcceptanceUseCase createStoreTermsAcceptanceUseCase =
      new CreateStoreTermsAcceptanceUseCase(
          merchantCommandPort,
          merchantStoreAuditPort,
          currentTenantProvider,
          currentAuthenticatedUserProvider);

  @Test
  void shouldPersistTermsAcceptanceWithRequestAcceptedAtAndNormalizedIp() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var acceptedAt = Instant.parse("2026-03-20T13:15:00Z");
    var request =
        new CreateStoreTermsAcceptanceCommand(
            LegalDocumentType.TERMS_OF_USE, "2026.03", acceptedAt);
    var output =
        new StoreTermsAcceptanceOutput(
            UUID.randomUUID(), LegalDocumentType.TERMS_OF_USE, "2026.03", acceptedAt);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(merchantCommandPort.createStoreTermsAcceptance(
            eq(storeId), eq(userId), eq(request), eq("203.0.113.9"), eq(acceptedAt)))
        .thenReturn(output);

    var response = createStoreTermsAcceptanceUseCase.execute(request, " 203.0.113.9 ");

    assertThat(
            java.util.Arrays.stream(request.getClass().getRecordComponents())
                .map(RecordComponent::getName))
        .containsExactly("documentType", "documentVersion", "acceptedAt");
    assertThat(response.documentVersion()).isEqualTo("2026.03");
    assertThat(response.acceptedAt()).isEqualTo(acceptedAt);
    verify(merchantCommandPort)
        .createStoreTermsAcceptance(storeId, userId, request, "203.0.113.9", acceptedAt);
    verify(merchantStoreAuditPort)
        .recordTermsAccepted(
            storeId,
            userId,
            output.id(),
            output.documentType(),
            output.documentVersion(),
            output.acceptedAt());
  }

  @Test
  void shouldRejectAcceptanceWithoutAuthenticatedUser() {
    var request =
        new CreateStoreTermsAcceptanceCommand(
            LegalDocumentType.TERMS_OF_USE, "2026.03", Instant.parse("2026-03-20T13:15:00Z"));

    when(currentAuthenticatedUserProvider.getRequiredUserId())
        .thenThrow(new AccessDeniedException("Unauthenticated request"));

    assertThatThrownBy(() -> createStoreTermsAcceptanceUseCase.execute(request, "203.0.113.9"))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Unauthenticated request");
    verifyNoInteractions(merchantCommandPort, merchantStoreAuditPort);
  }

  @Test
  void shouldPropagateCrossTenantAcceptanceAttempt() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var acceptedAt = Instant.parse("2026-03-20T13:15:00Z");
    var request =
        new CreateStoreTermsAcceptanceCommand(
            LegalDocumentType.TERMS_OF_USE, "2026.03", acceptedAt);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(merchantCommandPort.createStoreTermsAcceptance(
            eq(storeId), eq(userId), eq(request), eq("203.0.113.9"), eq(acceptedAt)))
        .thenThrow(new TenantAccessDeniedException());

    assertThatThrownBy(() -> createStoreTermsAcceptanceUseCase.execute(request, "203.0.113.9"))
        .isInstanceOf(TenantAccessDeniedException.class);
    verifyNoInteractions(merchantStoreAuditPort);
  }
}
