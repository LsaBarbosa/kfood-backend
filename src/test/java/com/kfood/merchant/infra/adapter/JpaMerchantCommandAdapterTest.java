package com.kfood.merchant.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.app.AuthenticatedUserNotFoundException;
import com.kfood.merchant.app.ChangeStoreStatusCommand;
import com.kfood.merchant.app.CreateDeliveryZoneCommand;
import com.kfood.merchant.app.CreateStoreCommand;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceCommand;
import com.kfood.merchant.app.DeliveryZoneAlreadyExistsException;
import com.kfood.merchant.app.InvalidStoreHoursException;
import com.kfood.merchant.app.OwnerAlreadyBoundToAnotherStoreException;
import com.kfood.merchant.app.StoreActivationRequirements;
import com.kfood.merchant.app.StoreActivationRequirementsNotMetException;
import com.kfood.merchant.app.StoreAddressCommand;
import com.kfood.merchant.app.StoreAddressOutput;
import com.kfood.merchant.app.StoreHourCommand;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreSlugAlreadyExistsException;
import com.kfood.merchant.app.TenantAccessDeniedException;
import com.kfood.merchant.app.UpdateDeliveryZoneCommand;
import com.kfood.merchant.app.UpdateStoreCommand;
import com.kfood.merchant.app.UpdateStoreHoursCommand;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreAddress;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptance;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

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
  void shouldCreateDeliveryZoneWithSuccess() {
    var store = activeStore();
    var storeId = store.getId();

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.existsByStoreIdAndZoneName(storeId, "Centro")).thenReturn(false);
    when(deliveryZoneRepository.saveAndFlush(any(DeliveryZone.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var output =
        adapter.createDeliveryZone(
            storeId,
            new CreateDeliveryZoneCommand(
                "  Centro  ", new BigDecimal("6.50"), new BigDecimal("25.00"), true));

    assertThat(output.zoneName()).isEqualTo("Centro");
    assertThat(output.feeAmount()).isEqualByComparingTo("6.50");
    assertThat(output.minOrderAmount()).isEqualByComparingTo("25.00");
    assertThat(output.active()).isTrue();
    verify(deliveryZoneRepository).saveAndFlush(any(DeliveryZone.class));
  }

  @Test
  void shouldThrowWhenCreatingDeliveryZoneForMissingStore() {
    var storeId = UUID.randomUUID();

    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.createDeliveryZone(
                    storeId,
                    new CreateDeliveryZoneCommand(
                        "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true)))
        .isInstanceOf(StoreNotFoundException.class);

    verifyNoInteractions(deliveryZoneRepository);
  }

  @Test
  void shouldRejectDeliveryZoneForSuspendedStore() {
    var store = suspendedStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.createDeliveryZone(
                    store.getId(),
                    new CreateDeliveryZoneCommand(
                        "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true)))
        .isInstanceOf(StoreNotActiveException.class);

    verifyNoInteractions(deliveryZoneRepository);
  }

  @Test
  void shouldRejectDuplicateDeliveryZone() {
    var store = activeStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.existsByStoreIdAndZoneName(store.getId(), "Centro"))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                adapter.createDeliveryZone(
                    store.getId(),
                    new CreateDeliveryZoneCommand(
                        "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true)))
        .isInstanceOf(DeliveryZoneAlreadyExistsException.class);

    verify(deliveryZoneRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldUpdateDeliveryZoneWithSuccess() {
    var store = activeStore();
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.findByIdAndStoreId(zone.getId(), store.getId()))
        .thenReturn(Optional.of(zone));
    when(deliveryZoneRepository.findByStoreIdAndZoneName(store.getId(), "Bairro Novo"))
        .thenReturn(Optional.empty());
    when(deliveryZoneRepository.saveAndFlush(zone)).thenReturn(zone);

    var output =
        adapter.updateDeliveryZone(
            store.getId(),
            zone.getId(),
            new UpdateDeliveryZoneCommand(
                "  Bairro Novo  ", new BigDecimal("8.00"), new BigDecimal("30.00"), false));

    assertThat(output.zoneName()).isEqualTo("Bairro Novo");
    assertThat(output.feeAmount()).isEqualByComparingTo("8.00");
    assertThat(output.minOrderAmount()).isEqualByComparingTo("30.00");
    assertThat(output.active()).isFalse();
  }

  @Test
  void shouldThrowWhenUpdatingDeliveryZoneForMissingStore() {
    var storeId = UUID.randomUUID();
    var zoneId = UUID.randomUUID();

    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.updateDeliveryZone(
                    storeId,
                    zoneId,
                    new UpdateDeliveryZoneCommand(
                        "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true)))
        .isInstanceOf(StoreNotFoundException.class);
  }

  @Test
  void shouldActivateDeliveryZoneOnUpdateWhenRequested() {
    var store = activeStore();
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            false);

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.findByIdAndStoreId(zone.getId(), store.getId()))
        .thenReturn(Optional.of(zone));
    when(deliveryZoneRepository.findByStoreIdAndZoneName(store.getId(), "Centro"))
        .thenReturn(Optional.of(zone));
    when(deliveryZoneRepository.saveAndFlush(zone)).thenReturn(zone);

    var output =
        adapter.updateDeliveryZone(
            store.getId(),
            zone.getId(),
            new UpdateDeliveryZoneCommand(
                "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true));

    assertThat(output.active()).isTrue();
  }

  @Test
  void shouldRejectUpdatingMissingDeliveryZone() {
    var store = activeStore();
    var zoneId = UUID.randomUUID();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.findByIdAndStoreId(zoneId, store.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.updateDeliveryZone(
                    store.getId(),
                    zoneId,
                    new UpdateDeliveryZoneCommand(
                        "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true)))
        .isInstanceOf(com.kfood.merchant.app.DeliveryZoneNotFoundException.class);
  }

  @Test
  void shouldRejectDuplicateDeliveryZoneOnUpdate() {
    var store = activeStore();
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);
    var duplicate =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            "Bairro Novo",
            new BigDecimal("7.00"),
            new BigDecimal("30.00"),
            true);

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.findByIdAndStoreId(zone.getId(), store.getId()))
        .thenReturn(Optional.of(zone));
    when(deliveryZoneRepository.findByStoreIdAndZoneName(store.getId(), "Bairro Novo"))
        .thenReturn(Optional.of(duplicate));

    assertThatThrownBy(
            () ->
                adapter.updateDeliveryZone(
                    store.getId(),
                    zone.getId(),
                    new UpdateDeliveryZoneCommand(
                        "Bairro Novo", new BigDecimal("8.00"), new BigDecimal("30.00"), true)))
        .isInstanceOf(DeliveryZoneAlreadyExistsException.class);
  }

  @Test
  void shouldDeleteDeliveryZoneLogically() {
    var store = activeStore();
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.findByIdAndStoreId(zone.getId(), store.getId()))
        .thenReturn(Optional.of(zone));
    when(deliveryZoneRepository.saveAndFlush(zone)).thenReturn(zone);

    adapter.deleteDeliveryZone(store.getId(), zone.getId());

    assertThat(zone.isActive()).isFalse();
    verify(deliveryZoneRepository).saveAndFlush(zone);
  }

  @Test
  void shouldThrowWhenDeletingDeliveryZoneForMissingStore() {
    var storeId = UUID.randomUUID();
    var zoneId = UUID.randomUUID();

    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.deleteDeliveryZone(storeId, zoneId))
        .isInstanceOf(StoreNotFoundException.class);
  }

  @Test
  void shouldRejectDeletingMissingDeliveryZone() {
    var store = activeStore();
    var zoneId = UUID.randomUUID();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.findByIdAndStoreId(zoneId, store.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.deleteDeliveryZone(store.getId(), zoneId))
        .isInstanceOf(com.kfood.merchant.app.DeliveryZoneNotFoundException.class);
  }

  @Test
  void shouldUpdateStoreHoursWithOpenHours() {
    var store = activeStore();
    var hours =
        List.of(
            new StoreHourCommand(
                DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false));

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);
    when(storeBusinessHourRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var output = adapter.updateStoreHours(store.getId(), new UpdateStoreHoursCommand(hours));

    assertThat(output.updated()).isTrue();
    assertThat(output.hoursVersion()).isEqualTo(1);
    verify(storeBusinessHourRepository).deleteAllByStoreId(store.getId());
    verify(storeBusinessHourRepository).saveAll(anyList());
    verify(storeRepository).saveAndFlush(store);
  }

  @Test
  void shouldUpdateStoreHoursWithClosedDay() {
    var store = activeStore();
    var hours = List.of(new StoreHourCommand(DayOfWeek.SUNDAY, null, null, true));

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    adapter.updateStoreHours(store.getId(), new UpdateStoreHoursCommand(hours));

    var captor = ArgumentCaptor.forClass(List.class);
    verify(storeBusinessHourRepository).saveAll(captor.capture());
    var savedHours = (List<StoreBusinessHour>) captor.getValue();
    assertThat(savedHours).singleElement().satisfies(hour -> assertThat(hour.isClosed()).isTrue());
  }

  @Test
  void shouldThrowWhenUpdatingHoursForMissingStore() {
    var storeId = UUID.randomUUID();

    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    storeId,
                    new UpdateStoreHoursCommand(
                        List.of(
                            new StoreHourCommand(
                                DayOfWeek.MONDAY,
                                LocalTime.of(10, 0),
                                LocalTime.of(22, 0),
                                false)))))
        .isInstanceOf(StoreNotFoundException.class);
  }

  @Test
  void shouldRejectUpdatingHoursForSuspendedStore() {
    var store = suspendedStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    store.getId(),
                    new UpdateStoreHoursCommand(
                        List.of(new StoreHourCommand(DayOfWeek.SUNDAY, null, null, true)))))
        .isInstanceOf(StoreNotActiveException.class);

    verify(storeBusinessHourRepository, never()).deleteAllByStoreId(any());
  }

  @Test
  void shouldRejectDuplicatedDaysWhenUpdatingHours() {
    var store = activeStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    store.getId(),
                    new UpdateStoreHoursCommand(
                        List.of(
                            new StoreHourCommand(
                                DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), false),
                            new StoreHourCommand(
                                DayOfWeek.MONDAY,
                                LocalTime.of(13, 0),
                                LocalTime.of(22, 0),
                                false)))))
        .isInstanceOf(InvalidStoreHoursException.class);
  }

  @Test
  void shouldRejectClosedDayWithTimesWhenUpdatingHours() {
    var store = activeStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    store.getId(),
                    new UpdateStoreHoursCommand(
                        List.of(
                            new StoreHourCommand(
                                DayOfWeek.MONDAY,
                                LocalTime.of(10, 0),
                                LocalTime.of(22, 0),
                                true)))))
        .isInstanceOf(InvalidStoreHoursException.class);
  }

  @Test
  void shouldRejectOpenDayWithoutOpenTimeWhenUpdatingHours() {
    var store = activeStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    store.getId(),
                    new UpdateStoreHoursCommand(
                        List.of(
                            new StoreHourCommand(
                                DayOfWeek.MONDAY, null, LocalTime.of(22, 0), false)))))
        .isInstanceOf(InvalidStoreHoursException.class);
  }

  @Test
  void shouldRejectOpenDayWithoutCloseTimeWhenUpdatingHours() {
    var store = activeStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    store.getId(),
                    new UpdateStoreHoursCommand(
                        List.of(
                            new StoreHourCommand(
                                DayOfWeek.MONDAY, LocalTime.of(10, 0), null, false)))))
        .isInstanceOf(InvalidStoreHoursException.class);
  }

  @Test
  void shouldRejectOpenTimeNotBeforeCloseTimeWhenUpdatingHours() {
    var store = activeStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.updateStoreHours(
                    store.getId(),
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
  void shouldUpdateStoreWithSuccess() {
    var store = activeStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(storeRepository.existsBySlugAndIdNot("novo-slug", store.getId())).thenReturn(false);
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var output =
        adapter.updateStore(
            store.getId(),
            new UpdateStoreCommand(
                "Novo Nome",
                "novo-slug",
                "31.662.365/0001-40",
                "21911112222",
                "UTC",
                StoreCategory.PIZZARIA,
                new StoreAddressCommand(
                    "25000-000", "Rua Central", "100", "Centro", "Mage", "RJ")));

    assertThat(output.name()).isEqualTo("Novo Nome");
    assertThat(output.slug()).isEqualTo("novo-slug");
    assertThat(output.cnpj()).isEqualTo("31.662.365/0001-40");
    assertThat(output.phone()).isEqualTo("21911112222");
    assertThat(output.timezone()).isEqualTo("UTC");
    assertThat(output.category()).isEqualTo(StoreCategory.PIZZARIA);
    assertThat(output.address())
        .isEqualTo(
            new StoreAddressOutput("25000000", "Rua Central", "100", "Centro", "Mage", "RJ"));
  }

  @Test
  void shouldThrowWhenUpdatingMissingStore() {
    var storeId = UUID.randomUUID();

    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.updateStore(
                    storeId, new UpdateStoreCommand("Novo Nome", null, null, null, null)))
        .isInstanceOf(StoreNotFoundException.class);
  }

  @Test
  void shouldRejectUpdatingSuspendedStore() {
    var store = suspendedStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.updateStore(
                    store.getId(), new UpdateStoreCommand("Novo Nome", null, null, null, null)))
        .isInstanceOf(StoreNotActiveException.class);

    verify(storeRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldRejectUpdatingStoreWithDuplicatedSlug() {
    var store = activeStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(storeRepository.existsBySlugAndIdNot("duplicado", store.getId())).thenReturn(true);

    assertThatThrownBy(
            () ->
                adapter.updateStore(
                    store.getId(), new UpdateStoreCommand(null, "duplicado", null, null, null)))
        .isInstanceOf(StoreSlugAlreadyExistsException.class);

    verify(storeRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldKeepNullAddressInOutputWhenUpdatingLegacyStoreWithoutAddress() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var output =
        adapter.updateStore(
            store.getId(), new UpdateStoreCommand("Novo Nome", null, null, null, null));

    assertThat(output.name()).isEqualTo("Novo Nome");
    assertThat(output.address()).isNull();
    assertThat(output.category()).isNull();
  }

  @Test
  void shouldCreateStoreForUnboundOwner() {
    var owner = owner(UUID.randomUUID(), null);
    var createdAt = Instant.parse("2026-03-20T10:15:00Z");

    when(identityUserRepository.findDetailedById(owner.getId())).thenReturn(Optional.of(owner));
    when(storeRepository.existsBySlug("nova-loja")).thenReturn(false);
    when(storeRepository.saveAndFlush(any(Store.class)))
        .thenAnswer(
            invocation -> {
              var savedStore = invocation.getArgument(0, Store.class);
              setCreatedAt(savedStore, createdAt);
              return savedStore;
            });
    when(identityUserRepository.saveAndFlush(owner)).thenReturn(owner);

    var output =
        adapter.createStore(
            owner.getId(),
            new CreateStoreCommand(
                "Nova Loja",
                "nova-loja",
                "45.723.174/0001-10",
                "21999990000",
                "America/Sao_Paulo",
                StoreCategory.PIZZARIA,
                new StoreAddressCommand(
                    "25000-000", "Rua Central", "100", "Centro", "Mage", "RJ")));

    assertThat(output.slug()).isEqualTo("nova-loja");
    assertThat(output.status()).isEqualTo(StoreStatus.SETUP);
    assertThat(output.createdAt()).isEqualTo(createdAt);
    assertThat(owner.getStoreId()).isEqualTo(output.id());
    var storeCaptor = ArgumentCaptor.forClass(Store.class);
    verify(storeRepository).saveAndFlush(storeCaptor.capture());
    assertThat(storeCaptor.getValue().getCategory()).isEqualTo(StoreCategory.PIZZARIA);
    assertThat(storeCaptor.getValue().getAddress().getZipCode()).isEqualTo("25000000");
    verify(identityUserRepository).saveAndFlush(owner);
  }

  @Test
  void shouldCreateStoreWithoutCategoryAndAddressWhenUsingLegacyCommandConstructor() {
    var owner = owner(UUID.randomUUID(), null);
    var createdAt = Instant.parse("2026-03-20T10:15:00Z");

    when(identityUserRepository.findDetailedById(owner.getId())).thenReturn(Optional.of(owner));
    when(storeRepository.existsBySlug("nova-loja")).thenReturn(false);
    when(storeRepository.saveAndFlush(any(Store.class)))
        .thenAnswer(
            invocation -> {
              var savedStore = invocation.getArgument(0, Store.class);
              setCreatedAt(savedStore, createdAt);
              return savedStore;
            });
    when(identityUserRepository.saveAndFlush(owner)).thenReturn(owner);

    var output =
        adapter.createStore(
            owner.getId(),
            new CreateStoreCommand(
                "Nova Loja",
                "nova-loja",
                "45.723.174/0001-10",
                "21999990000",
                "America/Sao_Paulo"));

    assertThat(output.slug()).isEqualTo("nova-loja");
    var storeCaptor = ArgumentCaptor.forClass(Store.class);
    verify(storeRepository).saveAndFlush(storeCaptor.capture());
    assertThat(storeCaptor.getValue().getCategory()).isNull();
    assertThat(storeCaptor.getValue().getAddress()).isNull();
  }

  @Test
  void shouldThrowWhenCreatingStoreForMissingUser() {
    var userId = UUID.randomUUID();

    when(identityUserRepository.findDetailedById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.createStore(
                    userId,
                    new CreateStoreCommand(
                        "Nova Loja",
                        "nova-loja",
                        "45.723.174/0001-10",
                        "21999990000",
                        "America/Sao_Paulo",
                        StoreCategory.PIZZARIA,
                        new StoreAddressCommand(
                            "25000-000", "Rua Central", "100", "Centro", "Mage", "RJ"))))
        .isInstanceOf(AuthenticatedUserNotFoundException.class);
  }

  @Test
  void shouldRejectOwnerAlreadyBoundToAnotherStore() {
    var owner = owner(UUID.randomUUID(), UUID.randomUUID());

    when(identityUserRepository.findDetailedById(owner.getId())).thenReturn(Optional.of(owner));

    assertThatThrownBy(
            () ->
                adapter.createStore(
                    owner.getId(),
                    new CreateStoreCommand(
                        "Nova Loja",
                        "nova-loja",
                        "45.723.174/0001-10",
                        "21999990000",
                        "America/Sao_Paulo",
                        StoreCategory.PIZZARIA,
                        new StoreAddressCommand(
                            "25000-000", "Rua Central", "100", "Centro", "Mage", "RJ"))))
        .isInstanceOf(OwnerAlreadyBoundToAnotherStoreException.class);
  }

  @Test
  void shouldRejectDuplicatedSlugOnCreateStore() {
    var owner = owner(UUID.randomUUID(), null);

    when(identityUserRepository.findDetailedById(owner.getId())).thenReturn(Optional.of(owner));
    when(storeRepository.existsBySlug("duplicado")).thenReturn(true);

    assertThatThrownBy(
            () ->
                adapter.createStore(
                    owner.getId(),
                    new CreateStoreCommand(
                        "Nova Loja",
                        "duplicado",
                        "45.723.174/0001-10",
                        "21999990000",
                        "America/Sao_Paulo",
                        StoreCategory.PIZZARIA,
                        new StoreAddressCommand(
                            "25000-000", "Rua Central", "100", "Centro", "Mage", "RJ"))))
        .isInstanceOf(StoreSlugAlreadyExistsException.class);

    verify(storeRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldActivateStoreWhenRequirementsAreMet() {
    var store = setupStore();
    var requirements = new StoreActivationRequirements(true, true, true, true, true);

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var output =
        adapter.changeStoreStatus(
            store.getId(), new ChangeStoreStatusCommand(StoreStatus.ACTIVE), requirements);

    assertThat(output.status()).isEqualTo(StoreStatus.ACTIVE);
    assertThat(output.category()).isEqualTo(StoreCategory.PIZZARIA);
    assertThat(output.address().city()).isEqualTo("Mage");
    assertThat(output.hoursConfigured()).isTrue();
    assertThat(output.deliveryZonesConfigured()).isTrue();
  }

  @Test
  void shouldRejectActivationWhenRequirementsAreNotMet() {
    var store = setupStore();
    var requirements = new StoreActivationRequirements(true, true, false, true, true);

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.changeStoreStatus(
                    store.getId(), new ChangeStoreStatusCommand(StoreStatus.ACTIVE), requirements))
        .isInstanceOf(StoreActivationRequirementsNotMetException.class);

    verify(storeRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldSuspendActiveStore() {
    var store = activeStore();
    var requirements = new StoreActivationRequirements(true, true, true, true, true);

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var output =
        adapter.changeStoreStatus(
            store.getId(), new ChangeStoreStatusCommand(StoreStatus.SUSPENDED), requirements);

    assertThat(output.status()).isEqualTo(StoreStatus.SUSPENDED);
  }

  @Test
  void shouldRejectChangingStatusToSetup() {
    var store = activeStore();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                adapter.changeStoreStatus(
                    store.getId(),
                    new ChangeStoreStatusCommand(StoreStatus.SETUP),
                    new StoreActivationRequirements(true, true, true, true, true)))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            });
  }

  @Test
  void shouldThrowWhenChangingStatusForMissingStore() {
    var storeId = UUID.randomUUID();

    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.changeStoreStatus(
                    storeId,
                    new ChangeStoreStatusCommand(StoreStatus.ACTIVE),
                    new StoreActivationRequirements(true, true, true, true, true)))
        .isInstanceOf(StoreNotFoundException.class);
  }

  @Test
  void shouldCreateStoreTermsAcceptanceWithTrimmedIp() {
    var store = activeStore();
    var user = owner(UUID.randomUUID(), store.getId());
    var acceptedAt = Instant.parse("2026-03-20T13:15:00Z");

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(identityUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(storeTermsAcceptanceRepository.saveAndFlush(any(StoreTermsAcceptance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var output =
        adapter.createStoreTermsAcceptance(
            store.getId(),
            user.getId(),
            new CreateStoreTermsAcceptanceCommand(LegalDocumentType.TERMS_OF_USE, "2026.03"),
            " 203.0.113.9 ",
            acceptedAt);

    assertThat(output.documentType()).isEqualTo(LegalDocumentType.TERMS_OF_USE);
    assertThat(output.documentVersion()).isEqualTo("2026.03");
    assertThat(output.acceptedAt()).isEqualTo(acceptedAt);

    var captor = ArgumentCaptor.forClass(StoreTermsAcceptance.class);
    verify(storeTermsAcceptanceRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getRequestIp()).isEqualTo("203.0.113.9");
  }

  @Test
  void shouldThrowWhenCreatingTermsAcceptanceForMissingStore() {
    var storeId = UUID.randomUUID();

    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.createStoreTermsAcceptance(
                    storeId,
                    UUID.randomUUID(),
                    new CreateStoreTermsAcceptanceCommand(
                        LegalDocumentType.TERMS_OF_USE, "2026.03"),
                    "203.0.113.9",
                    Instant.now()))
        .isInstanceOf(StoreNotFoundException.class);

    verifyNoInteractions(identityUserRepository, storeTermsAcceptanceRepository);
  }

  @Test
  void shouldThrowWhenCreatingTermsAcceptanceForMissingUser() {
    var store = activeStore();
    var userId = UUID.randomUUID();

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(identityUserRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.createStoreTermsAcceptance(
                    store.getId(),
                    userId,
                    new CreateStoreTermsAcceptanceCommand(
                        LegalDocumentType.TERMS_OF_USE, "2026.03"),
                    "203.0.113.9",
                    Instant.now()))
        .isInstanceOf(AuthenticatedUserNotFoundException.class);

    verifyNoInteractions(storeTermsAcceptanceRepository);
  }

  @Test
  void shouldRejectTermsAcceptanceFromAnotherTenant() {
    var store = activeStore();
    var user = owner(UUID.randomUUID(), UUID.randomUUID());

    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(identityUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

    assertThatThrownBy(
            () ->
                adapter.createStoreTermsAcceptance(
                    store.getId(),
                    user.getId(),
                    new CreateStoreTermsAcceptanceCommand(
                        LegalDocumentType.TERMS_OF_USE, "2026.03"),
                    "203.0.113.9",
                    Instant.now()))
        .isInstanceOf(TenantAccessDeniedException.class);

    verifyNoInteractions(storeTermsAcceptanceRepository);
  }

  private Store setupStore() {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo",
        StoreCategory.PIZZARIA,
        new StoreAddress("25000-000", "Rua Central", "100", "Centro", "Mage", "RJ"));
  }

  private Store activeStore() {
    var store = setupStore();
    store.activate();
    return store;
  }

  private Store suspendedStore() {
    var store = activeStore();
    store.suspend();
    return store;
  }

  private IdentityUserEntity owner(UUID userId, UUID storeId) {
    var user =
        new IdentityUserEntity(
            userId, storeId, "owner@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));
    return user;
  }

  private void setCreatedAt(Store store, Instant createdAt) {
    try {
      var field = store.getClass().getSuperclass().getDeclaredField("createdAt");
      field.setAccessible(true);
      field.set(store, createdAt);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
