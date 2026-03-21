package com.kfood.catalog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.catalog.app.CatalogCategoryNotFoundException;
import com.kfood.catalog.app.CatalogProductNotFoundException;
import com.kfood.catalog.app.CreateCatalogProductUseCase;
import com.kfood.catalog.app.DeactivateCatalogProductUseCase;
import com.kfood.catalog.app.ListCatalogProductsUseCase;
import com.kfood.catalog.app.UpdateCatalogProductPauseUseCase;
import com.kfood.catalog.app.UpdateCatalogProductUseCase;
import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "app.security.jwt-secret=12345678901234567890123456789012",
      "app.security.jwt-expiration-seconds=3600"
    })
class CatalogProductControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private CreateCatalogProductUseCase createCatalogProductUseCase;

  @MockitoBean private ListCatalogProductsUseCase listCatalogProductsUseCase;

  @MockitoBean private UpdateCatalogProductUseCase updateCatalogProductUseCase;

  @MockitoBean private UpdateCatalogProductPauseUseCase updateCatalogProductPauseUseCase;

  @MockitoBean private DeactivateCatalogProductUseCase deactivateCatalogProductUseCase;

  @Test
  void shouldCreateProductSuccessfully() throws Exception {
    var productId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    when(createCatalogProductUseCase.execute(any(CreateCatalogProductRequest.class)))
        .thenReturn(
            new CatalogProductResponse(
                productId,
                categoryId,
                "Pizza Calabresa",
                "Pizza com calabresa e cebola",
                new BigDecimal("39.90"),
                "https://cdn.kfood.local/pizza.jpg",
                20,
                true,
                false));

    mockMvc
        .perform(
            post("/v1/catalog/products")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": "%s",
                      "name": "Pizza Calabresa",
                      "description": "Pizza com calabresa e cebola",
                      "basePrice": 39.90,
                      "imageUrl": "https://cdn.kfood.local/pizza.jpg",
                      "sortOrder": 20,
                      "active": true,
                      "paused": false
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(productId.toString()))
        .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
        .andExpect(jsonPath("$.name").value("Pizza Calabresa"));
  }

  @Test
  void shouldListProductsForStore() throws Exception {
    when(listCatalogProductsUseCase.execute())
        .thenReturn(
            List.of(
                new CatalogProductResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Pizza Mussarela",
                    "Pizza com mussarela",
                    new BigDecimal("34.90"),
                    null,
                    10,
                    true,
                    false),
                new CatalogProductResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Pizza Calabresa",
                    "Pizza com calabresa",
                    new BigDecimal("39.90"),
                    null,
                    20,
                    false,
                    true)));

    mockMvc
        .perform(
            get("/v1/catalog/products")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Pizza Mussarela"))
        .andExpect(jsonPath("$[1].name").value("Pizza Calabresa"));
  }

  @Test
  void shouldUpdateProductSuccessfully() throws Exception {
    var productId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    when(updateCatalogProductUseCase.execute(
            any(UUID.class), any(UpdateCatalogProductRequest.class)))
        .thenReturn(
            new CatalogProductResponse(
                productId,
                categoryId,
                "Refrigerante",
                "Lata 350ml",
                new BigDecimal("7.50"),
                "https://cdn.kfood.local/refrigerante.jpg",
                30,
                false,
                true));

    mockMvc
        .perform(
            put("/v1/catalog/products/" + productId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": "%s",
                      "name": "Refrigerante",
                      "description": "Lata 350ml",
                      "basePrice": 7.50,
                      "imageUrl": "https://cdn.kfood.local/refrigerante.jpg",
                      "sortOrder": 30,
                      "active": false,
                      "paused": true
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Refrigerante"))
        .andExpect(jsonPath("$.paused").value(true));
  }

  @Test
  void shouldDeactivateProductSuccessfully() throws Exception {
    var productId = UUID.randomUUID();
    when(deactivateCatalogProductUseCase.execute(productId))
        .thenReturn(
            new CatalogProductResponse(
                productId,
                UUID.randomUUID(),
                "Pizza Calabresa",
                "Pizza com calabresa",
                new BigDecimal("39.90"),
                null,
                20,
                false,
                false));

    mockMvc
        .perform(
            patch("/v1/catalog/products/" + productId + "/inactive")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void shouldPauseProductSuccessfully() throws Exception {
    var productId = UUID.randomUUID();
    when(updateCatalogProductPauseUseCase.execute(
            org.mockito.ArgumentMatchers.eq(productId),
            any(UpdateCatalogProductPauseRequest.class)))
        .thenReturn(new CatalogProductPauseResponse(productId, true, true));

    mockMvc
        .perform(
            patch("/v1/catalog/products/" + productId + "/pause")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "paused": true,
                      "reason": "Ingredient unavailable"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(productId.toString()))
        .andExpect(jsonPath("$.paused").value(true))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void shouldReturnNotFoundWhenUpdatingMissingProduct() throws Exception {
    var productId = UUID.randomUUID();
    when(updateCatalogProductUseCase.execute(
            any(UUID.class), any(UpdateCatalogProductRequest.class)))
        .thenThrow(new CatalogProductNotFoundException(productId));

    mockMvc
        .perform(
            put("/v1/catalog/products/" + productId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": "%s",
                      "name": "Refrigerante",
                      "description": "Lata 350ml",
                      "basePrice": 7.50,
                      "imageUrl": "https://cdn.kfood.local/refrigerante.jpg",
                      "sortOrder": 30,
                      "active": false,
                      "paused": true
                    }
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
    var categoryId = UUID.randomUUID();
    when(createCatalogProductUseCase.execute(any(CreateCatalogProductRequest.class)))
        .thenThrow(new CatalogCategoryNotFoundException(categoryId));

    mockMvc
        .perform(
            post("/v1/catalog/products")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": "%s",
                      "name": "Pizza Calabresa",
                      "description": "Pizza com calabresa e cebola",
                      "basePrice": 39.90,
                      "imageUrl": "https://cdn.kfood.local/pizza.jpg",
                      "sortOrder": 20,
                      "active": true,
                      "paused": false
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldForbidAttendantFromCreatingProduct() throws Exception {
    mockMvc
        .perform(
            post("/v1/catalog/products")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId": "%s",
                      "name": "Pizza Calabresa",
                      "description": "Pizza com calabresa e cebola",
                      "basePrice": 39.90,
                      "imageUrl": "https://cdn.kfood.local/pizza.jpg",
                      "sortOrder": 20,
                      "active": true,
                      "paused": false
                    }
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  void shouldForbidAttendantFromPausingProduct() throws Exception {
    mockMvc
        .perform(
            patch("/v1/catalog/products/" + UUID.randomUUID() + "/pause")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "paused": true,
                      "reason": "Ingredient unavailable"
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  private String tokenOf(UserRoleName role) {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            role.name().toLowerCase() + "@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(role));
    return jwtTokenService.generateToken(user);
  }
}
