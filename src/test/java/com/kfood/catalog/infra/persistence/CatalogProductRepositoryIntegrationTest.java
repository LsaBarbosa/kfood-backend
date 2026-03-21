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
class CatalogProductRepositoryIntegrationTest extends PostgreSqlContainerIT {

  @Autowired private StoreRepository storeRepository;

  @Autowired private CatalogCategoryRepository catalogCategoryRepository;

  @Autowired private CatalogProductRepository catalogProductRepository;

  @Test
  @DisplayName("should persist valid product")
  void shouldPersistValidProduct() {
    var store = storeRepository.saveAndFlush(store("loja-do-bairro", "45.723.174/0001-10"));
    var category =
        catalogCategoryRepository.saveAndFlush(
            new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true));
    var product =
        new CatalogProduct(
            UUID.randomUUID(),
            store,
            category,
            "Pizza Calabresa",
            "Pizza com calabresa e cebola",
            new BigDecimal("39.90"),
            "https://cdn.kfood.local/pizza.jpg",
            20,
            true,
            false);

    var savedProduct = catalogProductRepository.saveAndFlush(product);

    assertThat(savedProduct.getId()).isNotNull();
    assertThat(savedProduct.getCreatedAt()).isNotNull();
    assertThat(savedProduct.getUpdatedAt()).isNotNull();
    assertThat(savedProduct.getCategory().getId()).isEqualTo(category.getId());
    assertThat(catalogProductRepository.existsByStoreId(store.getId())).isTrue();
  }

  @Test
  @DisplayName("should reject product without valid category")
  void shouldRejectProductWithoutValidCategory() {
    var store = storeRepository.saveAndFlush(store("loja-sem-categoria", "54.550.752/0001-55"));

    assertThatThrownBy(
            () ->
                catalogProductRepository.saveAndFlush(
                    new CatalogProduct(
                        UUID.randomUUID(),
                        store,
                        null,
                        "Pizza Calabresa",
                        "Pizza com calabresa e cebola",
                        new BigDecimal("39.90"),
                        null,
                        20,
                        true,
                        false)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("category is required");
  }

  @Test
  @DisplayName("should reject negative price")
  void shouldRejectNegativePrice() {
    var store = storeRepository.saveAndFlush(store("loja-preco", "45.723.174/0001-10"));
    var category =
        catalogCategoryRepository.saveAndFlush(
            new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true));

    assertThatThrownBy(
            () ->
                catalogProductRepository.saveAndFlush(
                    new CatalogProduct(
                        UUID.randomUUID(),
                        store,
                        category,
                        "Pizza Calabresa",
                        "Pizza com calabresa e cebola",
                        new BigDecimal("-1.00"),
                        null,
                        20,
                        true,
                        false)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("basePrice must be greater than or equal to zero");
  }

  @Test
  @DisplayName("should persist product belonging to category store")
  void shouldPersistProductBelongingToCategoryStore() {
    var store = storeRepository.saveAndFlush(store("loja-categoria", "54.550.752/0001-55"));
    var category =
        catalogCategoryRepository.saveAndFlush(
            new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true));
    var product =
        catalogProductRepository.saveAndFlush(
            new CatalogProduct(
                UUID.randomUUID(),
                store,
                category,
                "Pizza Mussarela",
                "Pizza com mussarela",
                new BigDecimal("34.90"),
                null,
                30,
                true,
                false));

    assertThat(product.getStore().getId()).isEqualTo(category.getStore().getId());
    assertThat(catalogProductRepository.findByIdAndStoreId(product.getId(), store.getId()))
        .isPresent();
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
