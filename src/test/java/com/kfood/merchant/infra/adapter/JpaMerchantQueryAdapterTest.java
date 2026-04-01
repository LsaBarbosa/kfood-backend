package com.kfood.merchant.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionGroupRepository;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreActivationRequirements;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaMerchantQueryAdapterTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final DeliveryZoneRepository deliveryZoneRepository = mock(DeliveryZoneRepository.class);
  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository =
      mock(StoreTermsAcceptanceRepository.class);
  private final CatalogCategoryRepository catalogCategoryRepository =
      mock(CatalogCategoryRepository.class);
  private final CatalogOptionGroupRepository catalogOptionGroupRepository =
      mock(CatalogOptionGroupRepository.class);
  private final CatalogProductRepository catalogProductRepository =
      mock(CatalogProductRepository.class);
  private final JpaMerchantQueryAdapter adapter =
      new JpaMerchantQueryAdapter(
          storeRepository,
          deliveryZoneRepository,
          storeBusinessHourRepository,
          storeTermsAcceptanceRepository,
          catalogCategoryRepository,
          catalogOptionGroupRepository,
          catalogProductRepository);

  @Test
  void shouldMapStoreDetailsHoursZonesAndTermsHistory() {
    var store = store();
    var storeId = store.getId();
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    var product =
        new CatalogProduct(
            UUID.randomUUID(),
            store,
            category,
            "Pizza Calabresa",
            "Descricao",
            new BigDecimal("39.90"),
            null,
            10,
            true,
            false);
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);
    var hour =
        StoreBusinessHour.open(store, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0));
    var acceptance =
        new StoreTermsAcceptance(
            UUID.randomUUID(),
            storeId,
            UUID.randomUUID(),
            com.kfood.merchant.domain.LegalDocumentType.TERMS_OF_USE,
            "2026.03",
            Instant.parse("2026-03-20T13:15:00Z"),
            "203.0.113.9");

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.findByIdAndStoreId(zone.getId(), storeId))
        .thenReturn(Optional.of(zone));
    when(deliveryZoneRepository.findAllByStoreIdOrderByZoneNameAsc(storeId))
        .thenReturn(List.of(zone));
    when(storeBusinessHourRepository.findByStoreId(storeId)).thenReturn(List.of(hour));
    when(storeTermsAcceptanceRepository.findAllByStoreIdOrderByAcceptedAtDesc(storeId))
        .thenReturn(List.of(acceptance));
    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(deliveryZoneRepository.findAllByStoreIdAndActiveTrueOrderByZoneNameAsc(storeId))
        .thenReturn(List.of(zone));
    when(catalogProductRepository.findAllVisibleForPublicMenu(any(), any(), any()))
        .thenReturn(List.of(product));
    when(catalogOptionGroupRepository.findAllByProduct_IdInAndActiveTrueOrderByProduct_IdAscIdAsc(
            any()))
        .thenReturn(List.of());

    assertThat(
            adapter
                .getStoreDetails(storeId, new StoreActivationRequirements(true, true, true))
                .status())
        .isEqualTo(store.getStatus());
    assertThat(adapter.getDeliveryZone(storeId, zone.getId()).zoneName()).isEqualTo("Centro");
    assertThat(adapter.listDeliveryZones(storeId)).hasSize(1);
    assertThat(adapter.getStoreHours(storeId).hours()).hasSize(1);
    assertThat(adapter.getPublicStore("loja-do-bairro").deliveryZones()).hasSize(1);
    assertThat(adapter.getPublicStoreMenu("loja-do-bairro").categories()).hasSize(1);
    assertThat(adapter.getStoreTermsAcceptanceHistory(storeId)).hasSize(1);
  }

  @Test
  void shouldMapPublicMenuOptionGroupsAndHandleEmptyProducts() {
    var store = store();
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    var product =
        new CatalogProduct(
            UUID.randomUUID(),
            store,
            category,
            "Pizza Calabresa",
            "Descricao",
            new BigDecimal("39.90"),
            null,
            10,
            true,
            false);
    var group = new CatalogOptionGroup(UUID.randomUUID(), product, "Bordas", 0, 1, false, true);
    group.addItem(
        new CatalogOptionItem(
            UUID.randomUUID(), group, "Catupiry", new BigDecimal("8.00"), true, 1));

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(catalogProductRepository.findAllVisibleForPublicMenu(any(), any(), any()))
        .thenReturn(List.of(product));
    when(catalogOptionGroupRepository.findAllByProduct_IdInAndActiveTrueOrderByProduct_IdAscIdAsc(
            any()))
        .thenReturn(List.of(group));

    var response = adapter.getPublicStoreMenu("loja-do-bairro");
    assertThat(response.categories().getFirst().products().getFirst().optionGroups()).hasSize(1);

    when(storeRepository.findBySlug("loja-vazia")).thenReturn(Optional.of(store));
    when(catalogProductRepository.findAllVisibleForPublicMenu(any(), any(), any()))
        .thenReturn(List.of());
    assertThat(adapter.getPublicStoreMenu("loja-vazia").categories()).isEmpty();
  }

  @Test
  void shouldThrowWhenStoreIsMissingForDetails() {
    var storeId = UUID.randomUUID();
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.getStoreDetails(storeId, new StoreActivationRequirements(true, true, true)))
        .isInstanceOf(StoreNotFoundException.class);
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
}
