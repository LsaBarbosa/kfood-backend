package com.kfood.merchant.infra.adapter;

import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionGroupRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.DeliveryZoneMapper;
import com.kfood.merchant.app.DeliveryZoneNotFoundException;
import com.kfood.merchant.app.DeliveryZoneOutput;
import com.kfood.merchant.app.MerchantViews;
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
    return DeliveryZoneMapper.toOutput(toDeliveryZoneView(zone));
  }

  @Override
  public List<DeliveryZoneOutput> listDeliveryZones(UUID storeId) {
    return deliveryZoneRepository.findAllByStoreIdOrderByZoneNameAsc(storeId).stream()
        .map(this::toDeliveryZoneView)
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
            .map(this::toStoreHourView)
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
            .map(this::toStoreHourView)
            .map(PublicStoreMapper::toHourOutput)
            .toList();
    var deliveryZones =
        deliveryZoneRepository
            .findAllByStoreIdAndActiveTrueOrderByZoneNameAsc(store.getId())
            .stream()
            .map(this::toDeliveryZoneView)
            .map(PublicStoreMapper::toDeliveryZoneOutput)
            .toList();
    return PublicStoreMapper.toOutput(toStoreView(store), hours, deliveryZones);
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
          .add(PublicStoreMapper.toMenuProductOutput(
              toPublicMenuProductView(
                  product, optionGroupsByProductId.getOrDefault(product.getId(), List.of()))));
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
        .map(this::toStoreTermsAcceptanceView)
        .map(StoreTermsAcceptanceMapper::toHistoryItemOutput)
        .toList();
  }

  private MerchantViews.StoreView toStoreView(com.kfood.merchant.infra.persistence.Store store) {
    return new MerchantViews.StoreView(
        store.getId(),
        store.getName(),
        store.getSlug(),
        store.getCnpj(),
        store.getPhone(),
        store.getTimezone(),
        store.getStatus(),
        store.getCreatedAt());
  }

  private MerchantViews.DeliveryZoneView toDeliveryZoneView(
      com.kfood.merchant.infra.persistence.DeliveryZone zone) {
    return new MerchantViews.DeliveryZoneView(
        zone.getId(),
        zone.getZoneName(),
        zone.getFeeAmount(),
        zone.getMinOrderAmount(),
        zone.isActive());
  }

  private MerchantViews.StoreHourView toStoreHourView(
      com.kfood.merchant.infra.persistence.StoreBusinessHour hour) {
    return new MerchantViews.StoreHourView(
        hour.getDayOfWeek(), hour.getOpenTime(), hour.getCloseTime(), hour.isClosed());
  }

  private MerchantViews.StoreTermsAcceptanceView toStoreTermsAcceptanceView(
      com.kfood.merchant.infra.persistence.StoreTermsAcceptance acceptance) {
    return new MerchantViews.StoreTermsAcceptanceView(
        acceptance.getId(),
        acceptance.getAcceptedByUserId(),
        acceptance.getDocumentType(),
        acceptance.getDocumentVersion(),
        acceptance.getAcceptedAt());
  }

  private MerchantViews.PublicStoreMenuProductView toPublicMenuProductView(
      CatalogProduct product, List<CatalogOptionGroup> optionGroups) {
    return new MerchantViews.PublicStoreMenuProductView(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getBasePrice(),
        product.getImageUrl(),
        product.isPaused(),
        optionGroups.stream()
            .filter(CatalogOptionGroup::isActive)
            .map(this::toPublicMenuOptionGroupView)
            .toList());
  }

  private MerchantViews.PublicStoreMenuOptionGroupView toPublicMenuOptionGroupView(
      CatalogOptionGroup group) {
    return new MerchantViews.PublicStoreMenuOptionGroupView(
        group.getId(),
        group.getName(),
        group.getMinSelect(),
        group.getMaxSelect(),
        group.isRequired(),
        group.getItems().stream()
            .filter(com.kfood.catalog.infra.persistence.CatalogOptionItem::isActive)
            .map(this::toPublicMenuOptionItemView)
            .toList());
  }

  private MerchantViews.PublicStoreMenuOptionItemView toPublicMenuOptionItemView(
      com.kfood.catalog.infra.persistence.CatalogOptionItem item) {
    return new MerchantViews.PublicStoreMenuOptionItemView(
        item.getId(), item.getName(), item.getExtraPrice(), item.getSortOrder());
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
