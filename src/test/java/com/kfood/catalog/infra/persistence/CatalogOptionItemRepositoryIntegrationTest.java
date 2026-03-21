package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
import java.math.BigDecimal;
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
class CatalogOptionItemRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private StoreRepository storeRepository;

  @Autowired private CatalogCategoryRepository catalogCategoryRepository;

  @Autowired private CatalogProductRepository catalogProductRepository;

  @Autowired private CatalogOptionGroupRepository catalogOptionGroupRepository;

  @Autowired private CatalogOptionItemRepository catalogOptionItemRepository;

  @Test
  @DisplayName("should persist valid option item through option group")
  void shouldPersistValidOptionItemThroughOptionGroup() {
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
    var optionItem =
        new CatalogOptionItem(
            UUID.randomUUID(), optionGroup, "Catupiry", new BigDecimal("8.00"), true, 10);
    optionGroup.addItem(optionItem);

    var savedOptionGroup = catalogOptionGroupRepository.saveAndFlush(optionGroup);
    var savedItems =
        catalogOptionItemRepository.findAllByOptionGroupIdOrderBySortOrderAscIdAsc(
            savedOptionGroup.getId());

    assertThat(savedItems).hasSize(1);
    assertThat(savedItems.get(0).getName()).isEqualTo("Catupiry");
    assertThat(savedItems.get(0).getExtraPrice()).isEqualByComparingTo("8.00");
  }

  @Test
  @DisplayName("should reject option item without existing group")
  void shouldRejectOptionItemWithoutExistingGroup() {
    assertThatThrownBy(
            () ->
                catalogOptionItemRepository.saveAndFlush(
                    new CatalogOptionItem(
                        UUID.randomUUID(), null, "Catupiry", new BigDecimal("8.00"), true, 10)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("optionGroup is required");
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
