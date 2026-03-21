package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import com.kfood.support.PostgreSqlContainerIT;
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
class CatalogCategoryRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private StoreRepository storeRepository;

  @Autowired private CatalogCategoryRepository catalogCategoryRepository;

  @Test
  @DisplayName("should persist valid category")
  void shouldPersistValidCategory() {
    var store = storeRepository.saveAndFlush(store("loja-do-bairro", "45.723.174/0001-10"));
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);

    var savedCategory = catalogCategoryRepository.saveAndFlush(category);

    assertThat(savedCategory.getId()).isNotNull();
    assertThat(savedCategory.getCreatedAt()).isNotNull();
    assertThat(savedCategory.getUpdatedAt()).isNotNull();
    assertThat(savedCategory.getStore().getId()).isEqualTo(store.getId());
    assertThat(catalogCategoryRepository.existsByStoreId(store.getId())).isTrue();
  }

  @Test
  @DisplayName("should persist category belonging to store")
  void shouldPersistCategoryBelongingToStore() {
    var store = storeRepository.saveAndFlush(store("loja-centro", "54.550.752/0001-55"));
    var category = new CatalogCategory(UUID.randomUUID(), store, "Bebidas", 20, true);

    var savedCategory = catalogCategoryRepository.saveAndFlush(category);

    assertThat(savedCategory.getStore().getId()).isEqualTo(store.getId());
    assertThat(catalogCategoryRepository.findByIdAndStoreId(savedCategory.getId(), store.getId()))
        .isPresent();
  }

  @Test
  @DisplayName("should not expose category from another tenant")
  void shouldNotExposeCategoryFromAnotherTenant() {
    var firstStore = storeRepository.saveAndFlush(store("loja-a", "54.550.752/0001-55"));
    var secondStore = storeRepository.saveAndFlush(store("loja-b", "45.723.174/0001-10"));

    var category =
        catalogCategoryRepository.saveAndFlush(
            new CatalogCategory(UUID.randomUUID(), firstStore, "Pizzas", 10, true));

    assertThat(catalogCategoryRepository.findByIdAndStoreId(category.getId(), secondStore.getId()))
        .isEmpty();
    assertThat(
            catalogCategoryRepository.findAllByStoreIdOrderBySortOrderAscNameAsc(
                secondStore.getId()))
        .isEmpty();
  }

  @Test
  @DisplayName("should reject duplicate category name within same store")
  void shouldRejectDuplicateCategoryNameWithinSameStore() {
    var store = storeRepository.saveAndFlush(store("loja-duplicada", "54.550.752/0001-55"));
    catalogCategoryRepository.saveAndFlush(
        new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true));

    assertThatThrownBy(
            () ->
                catalogCategoryRepository.saveAndFlush(
                    new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 20, true)))
        .isInstanceOf(Exception.class);
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
