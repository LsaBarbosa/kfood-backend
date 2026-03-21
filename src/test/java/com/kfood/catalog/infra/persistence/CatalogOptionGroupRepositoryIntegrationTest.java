package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
class CatalogOptionGroupRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private StoreRepository storeRepository;

  @Autowired private CatalogCategoryRepository catalogCategoryRepository;

  @Autowired private CatalogProductRepository catalogProductRepository;

  @Autowired private CatalogOptionGroupRepository catalogOptionGroupRepository;

  @Test
  @DisplayName("should persist valid option group")
  void shouldPersistValidOptionGroup() {
    var store = storeRepository.saveAndFlush(store("loja-do-bairro", "45.723.174/0001-10"));
    var category =
        catalogCategoryRepository.saveAndFlush(
            new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true));
    var product =
        catalogProductRepository.saveAndFlush(
            new CatalogProduct(
                UUID.randomUUID(),
                store,
                category,
                "Pizza Calabresa",
                "Pizza com calabresa e cebola",
                new BigDecimal("39.90"),
                null,
                20,
                true,
                false));
    var optionGroup =
        new CatalogOptionGroup(UUID.randomUUID(), product, "Stuffed Crust", 0, 1, false, true);
    optionGroup.addItem(
        new CatalogOptionItem(
            UUID.randomUUID(), optionGroup, "Catupiry", new BigDecimal("8.00"), true, 10));

    var savedOptionGroup = catalogOptionGroupRepository.saveAndFlush(optionGroup);

    assertThat(savedOptionGroup.getId()).isNotNull();
    assertThat(savedOptionGroup.getCreatedAt()).isNotNull();
    assertThat(savedOptionGroup.getUpdatedAt()).isNotNull();
    assertThat(savedOptionGroup.getProduct().getId()).isEqualTo(product.getId());
    assertThat(savedOptionGroup.getItems()).hasSize(1);
  }

  @Test
  @DisplayName("should reject option group without product")
  void shouldRejectOptionGroupWithoutProduct() {
    assertThatThrownBy(
            () ->
                catalogOptionGroupRepository.saveAndFlush(
                    new CatalogOptionGroup(
                        UUID.randomUUID(), null, "Stuffed Crust", 0, 1, false, true)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("product is required");
  }

  @Test
  @DisplayName("should list active option groups for product with items eagerly loaded")
  void shouldListActiveOptionGroupsForProductWithItemsEagerlyLoaded() {
    var store = storeRepository.saveAndFlush(store("loja-do-bairro", "45.723.174/0001-10"));
    var category =
        catalogCategoryRepository.saveAndFlush(
            new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true));
    var product =
        catalogProductRepository.saveAndFlush(
            new CatalogProduct(
                UUID.randomUUID(),
                store,
                category,
                "Pizza Calabresa",
                "Pizza com calabresa e cebola",
                new BigDecimal("39.90"),
                null,
                20,
                true,
                false));

    var activeGroup =
        new CatalogOptionGroup(UUID.randomUUID(), product, "Sauces", 0, 2, false, true);
    activeGroup.addItem(
        new CatalogOptionItem(
            UUID.randomUUID(), activeGroup, "Catupiry", new BigDecimal("8.00"), true, 10));
    var inactiveGroup =
        new CatalogOptionGroup(UUID.randomUUID(), product, "Stuffed Crust", 0, 1, false, false);
    inactiveGroup.addItem(
        new CatalogOptionItem(
            UUID.randomUUID(), inactiveGroup, "Cheddar", new BigDecimal("7.50"), true, 20));

    catalogOptionGroupRepository.saveAllAndFlush(List.of(inactiveGroup, activeGroup));

    var groups =
        catalogOptionGroupRepository.findAllByProduct_IdAndActiveTrueOrderByIdAsc(product.getId());

    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).getName()).isEqualTo("Sauces");
    assertThat(groups.get(0).getItems()).hasSize(1);
    assertThat(groups.get(0).getItems().get(0).getName()).isEqualTo("Catupiry");
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
