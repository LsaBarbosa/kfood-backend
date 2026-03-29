package com.kfood.merchant.infra.adapter;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.app.AuthenticatedUserNotFoundException;
import com.kfood.merchant.app.ChangeStoreStatusCommand;
import com.kfood.merchant.app.CreateDeliveryZoneCommand;
import com.kfood.merchant.app.CreateStoreCommand;
import com.kfood.merchant.app.CreateStoreOutput;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceCommand;
import com.kfood.merchant.app.DeliveryZoneAlreadyExistsException;
import com.kfood.merchant.app.DeliveryZoneMapper;
import com.kfood.merchant.app.DeliveryZoneOutput;
import com.kfood.merchant.app.InvalidStoreHoursException;
import com.kfood.merchant.app.OwnerAlreadyBoundToAnotherStoreException;
import com.kfood.merchant.app.StoreActivationRequirements;
import com.kfood.merchant.app.StoreActivationRequirementsNotMetException;
import com.kfood.merchant.app.StoreDetailsOutput;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOutput;
import com.kfood.merchant.app.StoreSlugAlreadyExistsException;
import com.kfood.merchant.app.StoreTermsAcceptanceOutput;
import com.kfood.merchant.app.TenantAccessDeniedException;
import com.kfood.merchant.app.UpdateStoreCommand;
import com.kfood.merchant.app.UpdateStoreHoursCommand;
import com.kfood.merchant.app.UpdateStoreHoursOutput;
import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptance;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JpaMerchantCommandAdapter implements MerchantCommandPort {

  private final StoreRepository storeRepository;
  private final DeliveryZoneRepository deliveryZoneRepository;
  private final StoreBusinessHourRepository storeBusinessHourRepository;
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository;
  private final IdentityUserRepository identityUserRepository;

  public JpaMerchantCommandAdapter(
      StoreRepository storeRepository,
      DeliveryZoneRepository deliveryZoneRepository,
      StoreBusinessHourRepository storeBusinessHourRepository,
      StoreTermsAcceptanceRepository storeTermsAcceptanceRepository,
      IdentityUserRepository identityUserRepository) {
    this.storeRepository = storeRepository;
    this.deliveryZoneRepository = deliveryZoneRepository;
    this.storeBusinessHourRepository = storeBusinessHourRepository;
    this.storeTermsAcceptanceRepository = storeTermsAcceptanceRepository;
    this.identityUserRepository = identityUserRepository;
  }

  @Override
  public DeliveryZoneOutput createDeliveryZone(UUID storeId, CreateDeliveryZoneCommand command) {
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    ensureStoreIsNotSuspended(store);
    var zoneName = command.zoneName().trim();

    if (deliveryZoneRepository.existsByStoreIdAndZoneName(storeId, zoneName)) {
      throw new DeliveryZoneAlreadyExistsException(zoneName);
    }

    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            zoneName,
            command.feeAmount(),
            command.minOrderAmount(),
            command.active());
    return DeliveryZoneMapper.toOutput(deliveryZoneRepository.saveAndFlush(zone));
  }

  @Override
  public UpdateStoreHoursOutput updateStoreHours(UUID storeId, UpdateStoreHoursCommand command) {
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    ensureStoreIsNotSuspended(store);
    validateHours(command.hours());

    storeBusinessHourRepository.deleteAllByStoreId(storeId);
    var newHours = command.hours().stream().map(hour -> toBusinessHour(store, hour)).toList();
    storeBusinessHourRepository.saveAll(newHours);

    store.incrementHoursVersion();
    storeRepository.saveAndFlush(store);
    return new UpdateStoreHoursOutput(true, store.getHoursVersion());
  }

  @Override
  public StoreOutput updateStore(UUID storeId, UpdateStoreCommand command) {
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    ensureStoreIsNotSuspended(store);

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

  @Override
  public CreateStoreOutput createStore(UUID authenticatedUserId, CreateStoreCommand command) {
    var authenticatedUser =
        identityUserRepository
            .findDetailedById(authenticatedUserId)
            .orElseThrow(() -> new AuthenticatedUserNotFoundException(authenticatedUserId));

    if (authenticatedUser.hasRole(UserRoleName.OWNER) && authenticatedUser.getStoreId() != null) {
      throw new OwnerAlreadyBoundToAnotherStoreException(authenticatedUser.getStoreId());
    }

    if (storeRepository.existsBySlug(command.slug())) {
      throw new StoreSlugAlreadyExistsException(command.slug());
    }

    var store =
        new Store(
            UUID.randomUUID(),
            command.name(),
            command.slug(),
            command.cnpj(),
            command.phone(),
            command.timezone());
    var savedStore = storeRepository.saveAndFlush(store);

    if (authenticatedUser.hasRole(UserRoleName.OWNER)) {
      authenticatedUser.bindToStore(savedStore.getId());
      identityUserRepository.saveAndFlush(authenticatedUser);
    }

    return new CreateStoreOutput(
        savedStore.getId(),
        savedStore.getSlug(),
        savedStore.getStatus(),
        savedStore.getCreatedAt());
  }

  @Override
  public StoreDetailsOutput changeStoreStatus(
      UUID storeId, ChangeStoreStatusCommand command, StoreActivationRequirements requirements) {
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    if (command.targetStatus() == StoreStatus.ACTIVE) {
      if (!requirements.canActivate()) {
        throw new StoreActivationRequirementsNotMetException(requirements.missingRequirements());
      }
      store.activate();
    } else if (command.targetStatus() == StoreStatus.SUSPENDED) {
      store.suspend();
    } else {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Changing status to SETUP is not allowed",
          HttpStatus.BAD_REQUEST);
    }

    var savedStore = storeRepository.saveAndFlush(store);
    return new StoreDetailsOutput(
        savedStore.getId(),
        savedStore.getSlug(),
        savedStore.getName(),
        savedStore.getStatus(),
        savedStore.getPhone(),
        savedStore.getTimezone(),
        requirements.hoursConfigured(),
        requirements.deliveryZonesConfigured());
  }

  @Override
  public StoreTermsAcceptanceOutput createStoreTermsAcceptance(
      UUID storeId,
      UUID authenticatedUserId,
      CreateStoreTermsAcceptanceCommand command,
      String requestIp,
      Instant acceptedAt) {
    storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    var authenticatedUser =
        identityUserRepository
            .findById(authenticatedUserId)
            .orElseThrow(() -> new AuthenticatedUserNotFoundException(authenticatedUserId));

    if (!Objects.equals(authenticatedUser.getStoreId(), storeId)) {
      throw new TenantAccessDeniedException();
    }

    var acceptance =
        new StoreTermsAcceptance(
            UUID.randomUUID(),
            storeId,
            authenticatedUserId,
            command.documentType(),
            command.documentVersion(),
            acceptedAt,
            normalizeRequestIp(requestIp));
    var savedAcceptance = storeTermsAcceptanceRepository.saveAndFlush(acceptance);
    return new StoreTermsAcceptanceOutput(
        savedAcceptance.getId(),
        savedAcceptance.getDocumentType(),
        savedAcceptance.getDocumentVersion(),
        savedAcceptance.getAcceptedAt());
  }

  private void ensureStoreIsNotSuspended(Store store) {
    if (store.isSuspended()) {
      throw new StoreNotActiveException(store.getId(), store.getStatus());
    }
  }

  private void validateHours(List<com.kfood.merchant.app.StoreHourCommand> hours) {
    var usedDays = EnumSet.noneOf(DayOfWeek.class);

    for (var hour : hours) {
      if (!usedDays.add(hour.dayOfWeek())) {
        throw new InvalidStoreHoursException("Duplicated dayOfWeek: " + hour.dayOfWeek());
      }

      if (hour.closed()) {
        if (hour.openTime() != null || hour.closeTime() != null) {
          throw new InvalidStoreHoursException(
              "Closed day must not define openTime or closeTime: " + hour.dayOfWeek());
        }
        continue;
      }

      if (hour.openTime() == null || hour.closeTime() == null) {
        throw new InvalidStoreHoursException(
            "Open day must define openTime and closeTime: " + hour.dayOfWeek());
      }

      if (!hour.openTime().isBefore(hour.closeTime())) {
        throw new InvalidStoreHoursException(
            "openTime must be before closeTime: " + hour.dayOfWeek());
      }
    }
  }

  private StoreBusinessHour toBusinessHour(
      Store store, com.kfood.merchant.app.StoreHourCommand hour) {
    if (hour.closed()) {
      return StoreBusinessHour.closed(store, hour.dayOfWeek());
    }

    return StoreBusinessHour.open(store, hour.dayOfWeek(), hour.openTime(), hour.closeTime());
  }

  private String normalizeRequestIp(String requestIp) {
    return Objects.requireNonNull(requestIp, "requestIp is required").trim();
  }
}
