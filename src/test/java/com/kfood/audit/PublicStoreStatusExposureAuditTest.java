package com.kfood.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.persistence.TestJpaAuditingConfig;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
class PublicStoreStatusExposureAuditTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StoreRepository storeRepository;
  @Autowired private CatalogCategoryRepository catalogCategoryRepository;
  @Autowired private CatalogProductRepository catalogProductRepository;

  @Test
  void shouldExposeSetupStoreAndMenuPublicly() throws Exception {
    var slug = "audit-setup-" + UUID.randomUUID();
    var store = storeRepository.saveAndFlush(store(slug, "45.723.174/0001-10"));
    seedMenu(store);

    mockMvc
        .perform(get("/v1/public/stores/{slug}", slug))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STORE_NOT_ACTIVE"));

    mockMvc
        .perform(get("/v1/public/stores/{slug}/menu", slug))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[0].products[0].name").value("Pizza Auditoria"));
  }

  @Test
  void shouldExposeSuspendedStoreAndMenuPublicly() throws Exception {
    var slug = "audit-suspended-" + UUID.randomUUID();
    var store = store(slug, "54.550.752/0001-55");
    store.activate();
    store.suspend();
    var savedStore = storeRepository.saveAndFlush(store);
    seedMenu(savedStore);

    mockMvc
        .perform(get("/v1/public/stores/{slug}", slug))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STORE_NOT_ACTIVE"));

    mockMvc
        .perform(get("/v1/public/stores/{slug}/menu", slug))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[0].products[0].name").value("Pizza Auditoria"));
  }

  private void seedMenu(Store store) {
    var category =
        catalogCategoryRepository.saveAndFlush(
            new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 1, true));
    catalogProductRepository.saveAndFlush(
        new CatalogProduct(
            UUID.randomUUID(),
            store,
            category,
            "Pizza Auditoria",
            "Produto visivel para auditoria",
            new BigDecimal("39.90"),
            null,
            1,
            true,
            false));
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja Auditoria", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
