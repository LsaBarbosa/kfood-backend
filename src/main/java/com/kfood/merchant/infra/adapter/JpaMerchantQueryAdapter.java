package com.kfood.merchant.infra.adapter;

import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionGroupRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.DeliveryZoneMapper;
import com.kfood.merchant.app.DeliveryZoneNotFoundException;
import com.kfood.merchant.app.DeliveryZoneOutput;
import com.kfood.merchant.app.PublicStoreMapper;
import com.kfood.merchant.app.PublicStoreMenuCategoryOutput;
import com.kfood.merchant.app.PublicStoreMenuOutput;
import com.kfood.merchant.app.PublicStoreMenuProductOutput;
import com.kfood.merchant.app.PublicStoreOutput;
import com.kfood.merchant.app.StoreActivationRequirements;
import com.kfood.merchant.app.StoreDetailsOutput;
import com.kfood.merchant.app.StoreHoursMapper;
import com.kfood.merchant.app.StoreHoursOutput;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.app.StoreTermsAcceptanceHistoryItemOutput;
import com.kfood.merchant.app.StoreTermsAcceptanceMapper;
import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaMerchantQueryAdapter implements MerchantQueryPort {

  private final StoreRepository storeRepository;
  private final DeliveryZoneRepository deliveryZoneRepository;
  private final StoreBusinessHourRepository storeBusinessHourRepository;
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository;
  private final CatalogCategoryRepository catalogCategoryRepository;
  private final CatalogOptionGroupRepository catalogOptionGroupRepository;
  private final CatalogProductRepository catalogProductRepository;

  public JpaMerchantQueryAdapter(
      StoreRepository storeRepository,
      DeliveryZoneRepository deliveryZoneRepository,
      StoreBusinessHourRepository storeBusinessHourRepository,
      StoreTermsAcceptanceRepository storeTermsAcceptanceRepository,
      CatalogCategoryRepository catalogCategoryRepository,
      CatalogOptionGroupRepository catalogOptionGroupRepository,
      CatalogProductRepository catalogProductRepository) {
    this.storeRepository = storeRepository;
    this.deliveryZoneRepository = deliveryZoneRepository;
    this.storeBusinessHourRepository = storeBusinessHourRepository;
    this.storeTermsAcceptanceRepository = storeTermsAcceptanceRepository;
    this.catalogCategoryRepository = catalogCategoryRepository;
    this.catalogOptionGroupRepository = catalogOptionGroupRepository;
    this.catalogProductRepository = catalogProductRepository;
  }

  @Override
  public StoreDetailsOutput getStoreDetails(
      UUID storeId, StoreActivationRequirements requirements) {
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    return new StoreDetailsOutput(
        store.getId(),
        store.getSlug(),
        store.getName(),
        store.getStatus(),
        store.getPhone(),
        store.getTimezone(),
        requirements.hoursConfigured(),
        requirements.deliveryZonesConfigured());
  }

  @Override
  public DeliveryZoneOutput getDeliveryZone(UUID storeId, UUID zoneId) {
    var zone =
        deliveryZoneRepository
            .findByIdAndStoreId(zoneId, storeId)
            .orElseThrow(() -> new DeliveryZoneNotFoundException(zoneId));
    return DeliveryZoneMapper.toOutput(zone);
  }

  @Override
  public List<DeliveryZoneOutput> listDeliveryZones(UUID storeId) {
    return deliveryZoneRepository.findAllByStoreIdOrderByZoneNameAsc(storeId).stream()
        .map(DeliveryZoneMapper::toOutput)
        .toList();
  }

  @Override
  public StoreHoursOutput getStoreHours(UUID storeId) {
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    var hours =
        storeBusinessHourRepository.findByStoreId(storeId).stream()
            .sorted(Comparator.comparingInt(item -> item.getDayOfWeek().getValue()))
            .map(StoreHoursMapper::toOutput)
            .toList();
    return new StoreHoursOutput(store.getHoursVersion(), hours);
  }

  @Override
  public PublicStoreOutput getPublicStore(String slug) {
    var store =
        storeRepository.findBySlug(slug).orElseThrow(() -> new StoreSlugNotFoundException(slug));
    var hours =
        storeBusinessHourRepository.findByStoreId(store.getId()).stream()
            .sorted(Comparator.comparingInt(item -> item.getDayOfWeek().getValue()))
            .map(PublicStoreMapper::toHourOutput)
            .toList();
    var deliveryZones =
        deliveryZoneRepository
            .findAllByStoreIdAndActiveTrueOrderByZoneNameAsc(store.getId())
            .stream()
            .map(PublicStoreMapper::toDeliveryZoneOutput)
            .toList();
    return PublicStoreMapper.toOutput(store, hours, deliveryZones);
  }

  @Override
  public PublicStoreMenuOutput getPublicStoreMenu(String slug) {
    var store =
        storeRepository.findBySlug(slug).orElseThrow(() -> new StoreSlugNotFoundException(slug));
    var categoriesById = new LinkedHashMap<UUID, PublicStoreMenuCategoryAccumulator>();
    var storeZoneId = ZoneId.of(store.getTimezone());
    var now = java.time.ZonedDateTime.now(storeZoneId);
    var products =
        catalogProductRepository.findAllVisibleForPublicMenu(
            store.getId(), now.getDayOfWeek(), now.toLocalTime());
    var optionGroupsByProductId = loadOptionGroupsByProductId(products);

    for (var product : products) {
      var category = product.getCategory();
      categoriesById
          .computeIfAbsent(
              category.getId(),
              ignored ->
                  new PublicStoreMenuCategoryAccumulator(category.getId(), category.getName()))
          .products()
          .add(
              PublicStoreMapper.toMenuProductOutput(
                  product, optionGroupsByProductId.getOrDefault(product.getId(), List.of())));
    }

    return new PublicStoreMenuOutput(
        categoriesById.values().stream()
            .map(
                category ->
                    new PublicStoreMenuCategoryOutput(
                        category.id(), category.name(), List.copyOf(category.products())))
            .toList());
  }

  @Override
  public List<StoreTermsAcceptanceHistoryItemOutput> getStoreTermsAcceptanceHistory(UUID storeId) {
    storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));
    return storeTermsAcceptanceRepository.findAllByStoreIdOrderByAcceptedAtDesc(storeId).stream()
        .map(StoreTermsAcceptanceMapper::toHistoryItemOutput)
        .toList();
  }

  private java.util.Map<UUID, List<CatalogOptionGroup>> loadOptionGroupsByProductId(
      List<CatalogProduct> products) {
    if (products.isEmpty()) {
      return java.util.Map.of();
    }

    var groupsByProductId = new LinkedHashMap<UUID, List<CatalogOptionGroup>>();
    var productIds = products.stream().map(CatalogProduct::getId).toList();
    for (var group :
        catalogOptionGroupRepository.findAllByProduct_IdInAndActiveTrueOrderByProduct_IdAscIdAsc(
            productIds)) {
      groupsByProductId
          .computeIfAbsent(group.getProduct().getId(), ignored -> new java.util.ArrayList<>())
          .add(group);
    }
    return groupsByProductId;
  }

  private record PublicStoreMenuCategoryAccumulator(
      UUID id, String name, List<PublicStoreMenuProductOutput> products) {

    private PublicStoreMenuCategoryAccumulator(UUID id, String name) {
      this(id, name, new java.util.ArrayList<>());
    }
  }
}
