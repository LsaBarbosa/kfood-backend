package com.kfood.catalog.app.selection;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.shared.exceptions.BusinessException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ProductOptionSelectionValidatorTest {

  private final CatalogOptionGroupLookup catalogOptionGroupLookup =
      mock(CatalogOptionGroupLookup.class);
  private final ObjectProvider<CatalogOptionGroupLookup> catalogOptionGroupLookupProvider =
      mock(ObjectProvider.class);
  private final ProductOptionSelectionValidator validator =
      new ProductOptionSelectionValidator(catalogOptionGroupLookupProvider);

  ProductOptionSelectionValidatorTest() {
    when(catalogOptionGroupLookupProvider.getIfAvailable()).thenReturn(catalogOptionGroupLookup);
  }

  @Test
  void shouldAcceptSelectionWithinMinAndMax() {
    var productId = UUID.randomUUID();
    var firstItemId = UUID.randomUUID();
    var secondItemId = UUID.randomUUID();
    var group =
        group(
            productId,
            "Stuffed Crust",
            1,
            2,
            true,
            List.of(activeItemFixture(firstItemId), activeItemFixture(secondItemId)));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatCode(
            () ->
                validator.validate(
                    productId,
                    List.of(
                        new OptionGroupSelectionInput(
                            group.getId(), List.of(firstItemId, secondItemId)))))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectWhenBelowMinimum() {
    var productId = UUID.randomUUID();
    var group =
        group(
            productId, "Stuffed Crust", 1, 2, true, List.of(activeItemFixture(UUID.randomUUID())));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatThrownBy(() -> validator.validate(productId, List.of()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("below minimum");
  }

  @Test
  void shouldRejectWhenAboveMaximum() {
    var productId = UUID.randomUUID();
    var firstItemId = UUID.randomUUID();
    var secondItemId = UUID.randomUUID();
    var group =
        group(
            productId,
            "Sauces",
            0,
            1,
            true,
            List.of(activeItemFixture(firstItemId), activeItemFixture(secondItemId)));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatThrownBy(
            () ->
                validator.validate(
                    productId,
                    List.of(
                        new OptionGroupSelectionInput(
                            group.getId(), List.of(firstItemId, secondItemId)))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("above maximum");
  }

  @Test
  void shouldAcceptEmptySelectionForOptionalGroup() {
    var productId = UUID.randomUUID();
    var group =
        group(productId, "Sauces", 0, 2, true, List.of(activeItemFixture(UUID.randomUUID())));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatCode(() -> validator.validate(productId, List.of())).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectWhenSelectionContainsUnknownGroup() {
    var productId = UUID.randomUUID();
    var group =
        group(productId, "Sauces", 0, 2, true, List.of(activeItemFixture(UUID.randomUUID())));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatThrownBy(
            () ->
                validator.validate(
                    productId,
                    List.of(
                        new OptionGroupSelectionInput(
                            UUID.randomUUID(), List.of(UUID.randomUUID())))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Option group does not belong to the product");
  }

  @Test
  void shouldRejectWhenSelectionGroupIdIsMissing() {
    var productId = UUID.randomUUID();

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                validator.validate(
                    productId, List.of(new OptionGroupSelectionInput(null, List.of()))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("optionGroupId must be informed");
  }

  @Test
  void shouldRejectDuplicateSelectionForSameGroup() {
    var productId = UUID.randomUUID();
    var group =
        group(productId, "Sauces", 0, 2, true, List.of(activeItemFixture(UUID.randomUUID())));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatThrownBy(
            () ->
                validator.validate(
                    productId,
                    List.of(
                        new OptionGroupSelectionInput(group.getId(), List.of()),
                        new OptionGroupSelectionInput(group.getId(), List.of()))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Duplicate selection for option group id");
  }

  @Test
  void shouldRejectDuplicateItemInsideSameGroup() {
    var productId = UUID.randomUUID();
    var itemId = UUID.randomUUID();
    var group = group(productId, "Sauces", 0, 2, true, List.of(activeItemFixture(itemId)));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatThrownBy(
            () ->
                validator.validate(
                    productId,
                    List.of(new OptionGroupSelectionInput(group.getId(), List.of(itemId, itemId)))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Duplicate option item id");
  }

  @Test
  void shouldRejectNullItemIdInsideSelection() {
    var productId = UUID.randomUUID();
    var group =
        group(productId, "Sauces", 0, 2, true, List.of(activeItemFixture(UUID.randomUUID())));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatThrownBy(
            () ->
                validator.validate(
                    productId,
                    List.of(new OptionGroupSelectionInput(group.getId(), itemIdsWithNull()))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("optionItemIds must not contain null values");
  }

  @Test
  void shouldRejectItemThatDoesNotBelongToGroupOrIsInactive() {
    var productId = UUID.randomUUID();
    var inactiveItemId = UUID.randomUUID();
    var group =
        group(productId, "Sauces", 0, 2, true, List.of(inactiveItemFixture(inactiveItemId)));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatThrownBy(
            () ->
                validator.validate(
                    productId,
                    List.of(new OptionGroupSelectionInput(group.getId(), List.of(inactiveItemId)))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("does not belong to group");
  }

  @Test
  void shouldIgnoreNullSelectionsList() {
    var productId = UUID.randomUUID();
    var group =
        group(productId, "Sauces", 0, 0, true, List.of(activeItemFixture(UUID.randomUUID())));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatCode(() -> validator.validate(productId, null)).doesNotThrowAnyException();
  }

  @Test
  void shouldIgnoreNullItemListInsideSelection() {
    var productId = UUID.randomUUID();
    var group =
        group(productId, "Sauces", 0, 1, true, List.of(activeItemFixture(UUID.randomUUID())));

    when(catalogOptionGroupLookup.findActiveByProductId(productId)).thenReturn(groups(group));

    assertThatCode(
            () ->
                validator.validate(
                    productId, List.of(new OptionGroupSelectionInput(group.getId(), null))))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldFailFastWhenLookupBeanIsUnavailable() {
    when(catalogOptionGroupLookupProvider.getIfAvailable()).thenReturn(null);

    assertThatThrownBy(() -> validator.validate(UUID.randomUUID(), List.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("CatalogOptionGroupLookup bean is required");
  }

  private CatalogOptionGroup group(
      UUID productId,
      String name,
      int minSelect,
      int maxSelect,
      boolean active,
      List<OptionItemFixture> items) {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    var product =
        new CatalogProduct(
            productId,
            store,
            category,
            "Pizza Calabresa",
            "Pizza com calabresa e cebola",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);
    var group =
        new CatalogOptionGroup(
            UUID.randomUUID(), product, name, minSelect, maxSelect, false, active);
    items.stream()
        .map(
            item ->
                new CatalogOptionItem(
                    item.id(), group, "Catupiry", new BigDecimal("8.00"), item.active(), 10))
        .forEach(group::addItem);
    return group;
  }

  private List<CatalogOptionGroupView> groups(CatalogOptionGroup group) {
    return List.of(group);
  }

  private OptionItemFixture activeItemFixture(UUID itemId) {
    return new OptionItemFixture(itemId, true);
  }

  private OptionItemFixture inactiveItemFixture(UUID itemId) {
    return new OptionItemFixture(itemId, false);
  }

  private List<UUID> itemIdsWithNull() {
    var itemIds = new ArrayList<UUID>();
    itemIds.add(null);
    return itemIds;
  }

  private record OptionItemFixture(UUID id, boolean active) {}
}
