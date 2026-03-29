package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicStoreMapperTest {

  @Test
  void shouldMapPublicStoreBoundaryObjects() {
    var store = store();
    store.activate();
    var hour =
        StoreBusinessHour.open(store, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0));
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);

    var mappedHour = PublicStoreMapper.toHourOutput(hour);
    var mappedZone = PublicStoreMapper.toDeliveryZoneOutput(zone);
    var output = PublicStoreMapper.toOutput(store, List.of(mappedHour), List.of(mappedZone));

    assertThat(mappedHour.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(mappedHour.openTime()).isEqualTo(LocalTime.of(10, 0));
    assertThat(mappedZone.zoneName()).isEqualTo("Centro");
    assertThat(output.slug()).isEqualTo("loja-do-bairro");
    assertThat(output.status()).isEqualTo(StoreStatus.ACTIVE);
    assertThat(output.deliveryZones()).containsExactly(mappedZone);
  }

  @Test
  void shouldMapOptionGroupAndItem() {
    var store = store();
    var category = category(store);
    var product = product(store, category, "Pizza Calabresa");
    var group = optionGroup(product, "Bordas", 1, 2, true, true);
    group.addItem(optionItem(group, "Catupiry", "8.00", true, 10));

    var response = PublicStoreMapper.toMenuProductOutput(product);

    assertThat(response.optionGroups()).hasSize(1);
    var mappedGroup = response.optionGroups().getFirst();
    assertThat(mappedGroup.name()).isEqualTo("Bordas");
    assertThat(mappedGroup.minSelect()).isEqualTo(1);
    assertThat(mappedGroup.maxSelect()).isEqualTo(2);
    assertThat(mappedGroup.required()).isTrue();
    assertThat(mappedGroup.items()).hasSize(1);
    assertThat(mappedGroup.items().getFirst().name()).isEqualTo("Catupiry");
    assertThat(mappedGroup.items().getFirst().extraPrice()).isEqualByComparingTo("8.00");
    assertThat(mappedGroup.items().getFirst().sortOrder()).isEqualTo(10);
  }

  @Test
  void shouldMapSelectionRulesAndFilterInactiveItems() {
    var store = store();
    var category = category(store);
    var product = product(store, category, "Pizza Frango");
    var group = optionGroup(product, "Molhos", 0, 3, false, true);
    group.addItem(optionItem(group, "Barbecue", "2.50", true, 5));
    group.addItem(optionItem(group, "Alho", "1.50", false, 6));

    var response = PublicStoreMapper.toMenuProductOutput(product);

    assertThat(response.optionGroups()).hasSize(1);
    var mappedGroup = response.optionGroups().getFirst();
    assertThat(mappedGroup.minSelect()).isZero();
    assertThat(mappedGroup.maxSelect()).isEqualTo(3);
    assertThat(mappedGroup.required()).isFalse();
    assertThat(mappedGroup.items()).extracting(item -> item.name()).containsExactly("Barbecue");
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

  private CatalogCategory category(Store store) {
    return new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
  }

  private CatalogProduct product(Store store, CatalogCategory category, String name) {
    return new CatalogProduct(
        UUID.randomUUID(),
        store,
        category,
        name,
        "Descricao",
        new BigDecimal("39.90"),
        null,
        10,
        true,
        false);
  }

  private CatalogOptionGroup optionGroup(
      CatalogProduct product,
      String name,
      int minSelect,
      int maxSelect,
      boolean required,
      boolean active) {
    var group =
        new CatalogOptionGroup(
            UUID.randomUUID(), product, name, minSelect, maxSelect, required, active);
    attachOptionGroup(product, group);
    return group;
  }

  private CatalogOptionItem optionItem(
      CatalogOptionGroup group, String name, String extraPrice, boolean active, int sortOrder) {
    return new CatalogOptionItem(
        UUID.randomUUID(), group, name, new BigDecimal(extraPrice), active, sortOrder);
  }

  private void attachOptionGroup(CatalogProduct product, CatalogOptionGroup group) {
    try {
      var field = CatalogProduct.class.getDeclaredField("optionGroups");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      var groups = (java.util.List<CatalogOptionGroup>) field.get(product);
      groups.add(group);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
