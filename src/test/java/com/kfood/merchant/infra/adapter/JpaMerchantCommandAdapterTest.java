package com.kfood.merchant.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.app.ChangeStoreStatusCommand;
import com.kfood.merchant.app.CreateDeliveryZoneCommand;
import com.kfood.merchant.app.CreateStoreCommand;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceCommand;
import com.kfood.merchant.app.DeliveryZoneAlreadyExistsException;
import com.kfood.merchant.app.InvalidStoreHoursException;
import com.kfood.merchant.app.OwnerAlreadyBoundToAnotherStoreException;
import com.kfood.merchant.app.StoreActivationRequirements;
import com.kfood.merchant.app.StoreActivationRequirementsNotMetException;
import com.kfood.merchant.app.StoreHourCommand;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreSlugAlreadyExistsException;
import com.kfood.merchant.app.StoreTermsAcceptanceOutput;
import com.kfood.merchant.app.TenantAccessDeniedException;
import com.kfood.merchant.app.UpdateStoreCommand;
import com.kfood.merchant.app.UpdateStoreHoursCommand;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptance;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaMerchantCommandAdapterTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository =
      mock(StoreTermsAcceptanceRepository.class);
  private final IdentityUserRepository identityUserRepository = mock(IdentityUserRepository.class);
  private final JpaMerchantCommandAdapter adapter =
      new JpaMerchantCommandAdapter(
          storeRepository,
          deliveryZoneRepository,
          storeBusinessHourRepository,
          storeTermsAcceptanceRepository,
          identityUserRepository);

  @Test
  void shouldHandleCoreCommandFlows() {
    var store = store();
    var storeId = store.getId();
    var owner = owner(UUID.randomUUID(), null);

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.existsByStoreIdAndZoneName(storeId, "Centro")).thenReturn(false);
    when(deliveryZoneRepository.saveAndFlush(any(DeliveryZone.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(storeRepository.saveAndFlush(any(Store.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);
    when(identityUserRepository.findDetailedById(owner.getId())).thenReturn(Optional.of(owner));
    when(storeRepository.existsBySlug("nova-loja")).thenReturn(false);
    when(identityUserRepository.findById(owner.getId()))
        .thenReturn(Optional.of(owner(owner.getId(), storeId)));
    when(storeTermsAcceptanceRepository.saveAndFlush(any(StoreTermsAcceptance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(
            adapter
                .createDeliveryZone(
                    storeId,
                    new CreateDeliveryZoneCommand(
                        "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true))
                .zoneName())
        .isEqualTo("Centro");

    assertThat(
            adapter
                .updateStoreHours(
                    storeId,
                    new UpdateStoreHoursCommand(
                        List.of(
                            new StoreHourCommand(
                                DayOfWeek.MONDAY,
                                LocalTime.of(10, 0),
                                LocalTime.of(22, 0),
                                false))))
                .updated())
        .isTrue();
    verify(storeBusinessHourRepository).saveAll(anyList());

    assertThat(
            adapter
                .updateStore(
                    storeId, new UpdateStoreCommand("Loja Premium", null, null, null, null))
                .name())
        .isEqualTo("Loja Premium");

    assertThat(
            adapter
                .createStore(
                    owner.getId(),
                    new CreateStoreCommand(
                        "Nova Loja",
                        "nova-loja",
                        "45.723.174/0001-10",
                        "21999990000",
                        "America/Sao_Paulo"))
                .slug())
        .isEqualTo("nova-loja");

    assertThat(
            adapter
                .changeStoreStatus(
                    storeId,
                    new ChangeStoreStatusCommand(StoreStatus.ACTIVE),
                    new StoreActivationRequirements(true, true, true))
                .status())
        .isEqualTo(StoreStatus.ACTIVE);

    StoreTermsAcceptanceOutput acceptance =
        adapter.createStoreTermsAcceptance(
            storeId,
            owner.getId(),
            new CreateStoreTermsAcceptanceCommand(LegalDocumentType.TERMS_OF_USE, "2026.03"),
            "203.0.113.9",
            Instant.parse("2026-03-20T13:15:00Z"));
    assertThat(acceptance.documentVersion()).isEqualTo("2026.03");
  }

  @Test
  void shouldRejectInvalidCommandBranches() {
    var store = store();
    var storeId = store.getId();

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeRepository.existsBySlugAndIdNot("duplicado", storeId)).thenReturn(true);

    assertThatThrownBy(
            () ->
                adapter.updateStore(
                    storeId, new UpdateStoreCommand(null, "duplicado", null, null, null)))
        .isInstanceOf(StoreSlugAlreadyExistsException.class);

    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    storeId,
                    new UpdateStoreHoursCommand(
                        List.of(
                            new StoreHourCommand(
                                DayOfWeek.MONDAY,
                                LocalTime.of(22, 0),
                                LocalTime.of(10, 0),
                                false)))))
        .isInstanceOf(InvalidStoreHoursException.class);
  }

  @Test
  void shouldCoverRemainingMerchantCommandBranches() {
    var store = store();
    var storeId = store.getId();
    var suspendedStore = store();
    suspendedStore.activate();
    suspendedStore.suspend();
    var activeStore = store();
    activeStore.activate();
    var ownerId = UUID.randomUUID();

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.existsByStoreIdAndZoneName(storeId, "Centro")).thenReturn(true);
    assertThatThrownBy(
            () ->
                adapter.createDeliveryZone(
                    storeId,
                    new CreateDeliveryZoneCommand(
                        "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true)))
        .isInstanceOf(DeliveryZoneAlreadyExistsException.class);

    when(storeRepository.findById(suspendedStore.getId())).thenReturn(Optional.of(suspendedStore));
    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    suspendedStore.getId(),
                    new UpdateStoreHoursCommand(
                        List.of(new StoreHourCommand(DayOfWeek.SUNDAY, null, null, true)))))
        .isInstanceOf(StoreNotActiveException.class);

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    storeId,
                    new UpdateStoreHoursCommand(
                        List.of(
                            new StoreHourCommand(
                                DayOfWeek.MONDAY, null, LocalTime.of(22, 0), true)))))
        .isInstanceOf(InvalidStoreHoursException.class);
    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    storeId,
                    new UpdateStoreHoursCommand(
                        List.of(
                            new StoreHourCommand(DayOfWeek.MONDAY, null, null, false),
                            new StoreHourCommand(
                                DayOfWeek.MONDAY,
                                LocalTime.of(10, 0),
                                LocalTime.of(22, 0),
                                false)))))
        .isInstanceOf(InvalidStoreHoursException.class);

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeRepository.saveAndFlush(any(Store.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var updated =
        adapter.updateStore(
            storeId,
            new UpdateStoreCommand(
                "Novo Nome", "novo-slug", "31.662.365/0001-40", "21911112222", "UTC"));
    assertThat(updated.slug()).isEqualTo("novo-slug");

    var boundOwner = owner(ownerId, UUID.randomUUID());
    when(identityUserRepository.findDetailedById(ownerId)).thenReturn(Optional.of(boundOwner));
    assertThatThrownBy(
            () ->
                adapter.createStore(
                    ownerId,
                    new CreateStoreCommand(
                        "Nova Loja",
                        "nova-loja-2",
                        "45.723.174/0001-10",
                        "21999990000",
                        "America/Sao_Paulo")))
        .isInstanceOf(OwnerAlreadyBoundToAnotherStoreException.class);

    var ownerWithoutStore = owner(ownerId, null);
    when(identityUserRepository.findDetailedById(ownerId))
        .thenReturn(Optional.of(ownerWithoutStore));
    when(storeRepository.existsBySlug("duplicado")).thenReturn(true);
    assertThatThrownBy(
            () ->
                adapter.createStore(
                    ownerId,
                    new CreateStoreCommand(
                        "Nova Loja",
                        "duplicado",
                        "45.723.174/0001-10",
                        "21999990000",
                        "America/Sao_Paulo")))
        .isInstanceOf(StoreSlugAlreadyExistsException.class);

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    assertThatThrownBy(
            () ->
                adapter.changeStoreStatus(
                    storeId,
                    new ChangeStoreStatusCommand(StoreStatus.ACTIVE),
                    new StoreActivationRequirements(false, true, true)))
        .isInstanceOf(StoreActivationRequirementsNotMetException.class);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore));
    assertThat(
            adapter
                .changeStoreStatus(
                    storeId,
                    new ChangeStoreStatusCommand(StoreStatus.SUSPENDED),
                    new StoreActivationRequirements(true, true, true))
                .status())
        .isEqualTo(StoreStatus.SUSPENDED);
    assertThatThrownBy(
            () ->
                adapter.changeStoreStatus(
                    storeId,
                    new ChangeStoreStatusCommand(StoreStatus.SETUP),
                    new StoreActivationRequirements(true, true, true)))
        .isInstanceOf(com.kfood.shared.exceptions.BusinessException.class);

    when(identityUserRepository.findById(ownerId))
        .thenReturn(Optional.of(owner(ownerId, UUID.randomUUID())));
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    assertThatThrownBy(
            () ->
                adapter.createStoreTermsAcceptance(
                    storeId,
                    ownerId,
                    new CreateStoreTermsAcceptanceCommand(
                        LegalDocumentType.TERMS_OF_USE, "2026.03"),
                    "203.0.113.9",
                    Instant.parse("2026-03-20T13:15:00Z")))
        .isInstanceOf(TenantAccessDeniedException.class);
  }

  private Store store() {
    return new Store(
        UUID.randomUUID(),
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
