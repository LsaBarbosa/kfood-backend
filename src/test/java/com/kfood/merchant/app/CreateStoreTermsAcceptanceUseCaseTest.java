package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.api.CreateStoreTermsAcceptanceRequest;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptance;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

class CreateStoreTermsAcceptanceUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository =
      mock(StoreTermsAcceptanceRepository.class);
  private final IdentityUserRepository identityUserRepository = mock(IdentityUserRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-20T13:15:00Z"), ZoneOffset.UTC);
  private final CreateStoreTermsAcceptanceUseCase createStoreTermsAcceptanceUseCase =
      new CreateStoreTermsAcceptanceUseCase(
          storeRepository,
          storeTermsAcceptanceRepository,
          identityUserRepository,
          currentTenantProvider,
          currentAuthenticatedUserProvider,
          clock);

  @Test
  void shouldPersistTermsAcceptanceWithServerGeneratedTimestampIpVersionUserAndStore() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var request = new CreateStoreTermsAcceptanceRequest(LegalDocumentType.TERMS_OF_USE, "2026.03");
    var store = store(storeId);
    var user = owner(userId, storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(identityUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(storeTermsAcceptanceRepository.saveAndFlush(any(StoreTermsAcceptance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = createStoreTermsAcceptanceUseCase.execute(request, "203.0.113.9");

    var captor = ArgumentCaptor.forClass(StoreTermsAcceptance.class);
    verify(storeTermsAcceptanceRepository).saveAndFlush(captor.capture());
    var saved = captor.getValue();

    assertThat(response.documentType()).isEqualTo(LegalDocumentType.TERMS_OF_USE);
    assertThat(response.documentVersion()).isEqualTo("2026.03");
    assertThat(response.acceptedAt()).isEqualTo(Instant.parse("2026-03-20T13:15:00Z"));
    assertThat(saved.getStoreId()).isEqualTo(storeId);
    assertThat(saved.getAcceptedByUserId()).isEqualTo(userId);
    assertThat(saved.getDocumentVersion()).isEqualTo("2026.03");
    assertThat(saved.getAcceptedAt()).isEqualTo(Instant.parse("2026-03-20T13:15:00Z"));
    assertThat(saved.getRequestIp()).isEqualTo("203.0.113.9");
  }

  @Test
  void shouldCreateNewAcceptanceWhenVersionChanges() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var store = store(storeId);
    var user = owner(userId, storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(identityUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(storeTermsAcceptanceRepository.saveAndFlush(any(StoreTermsAcceptance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var firstResponse =
        createStoreTermsAcceptanceUseCase.execute(
            new CreateStoreTermsAcceptanceRequest(LegalDocumentType.TERMS_OF_USE, "2026.03"),
            "203.0.113.9");

    var secondResponse =
        createStoreTermsAcceptanceUseCase.execute(
            new CreateStoreTermsAcceptanceRequest(LegalDocumentType.TERMS_OF_USE, "2026.04"),
            "203.0.113.10");

    assertThat(secondResponse.id()).isNotEqualTo(firstResponse.id());
    assertThat(secondResponse.documentVersion()).isEqualTo("2026.04");
  }

  @Test
  void shouldRejectAcceptanceWithoutAuthenticatedUser() {
    var request = new CreateStoreTermsAcceptanceRequest(LegalDocumentType.TERMS_OF_USE, "2026.03");

    when(currentAuthenticatedUserProvider.getRequiredUserId())
        .thenThrow(new AccessDeniedException("Unauthenticated request"));

    assertThatThrownBy(() -> createStoreTermsAcceptanceUseCase.execute(request, "203.0.113.9"))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Unauthenticated request");
  }

  @Test
  void shouldRejectCrossTenantAcceptanceAttempt() {
    var storeId = UUID.randomUUID();
    var anotherStoreId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var request = new CreateStoreTermsAcceptanceRequest(LegalDocumentType.TERMS_OF_USE, "2026.03");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store(storeId)));
    when(identityUserRepository.findById(userId))
        .thenReturn(Optional.of(owner(userId, anotherStoreId)));

    assertThatThrownBy(() -> createStoreTermsAcceptanceUseCase.execute(request, "203.0.113.9"))
        .isInstanceOf(TenantAccessDeniedException.class)
        .hasMessageContaining("another tenant");
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var request = new CreateStoreTermsAcceptanceRequest(LegalDocumentType.TERMS_OF_USE, "2026.03");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> createStoreTermsAcceptanceUseCase.execute(request, "203.0.113.9"))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldThrowWhenAuthenticatedUserDoesNotExist() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var request = new CreateStoreTermsAcceptanceRequest(LegalDocumentType.TERMS_OF_USE, "2026.03");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store(storeId)));
    when(identityUserRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> createStoreTermsAcceptanceUseCase.execute(request, "203.0.113.9"))
        .isInstanceOf(AuthenticatedUserNotFoundException.class)
        .hasMessageContaining(userId.toString());
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

  private IdentityUserEntity owner(UUID userId, UUID storeId) {
    var user =
        new IdentityUserEntity(
            userId, storeId, "owner@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));
    return user;
  }
}
