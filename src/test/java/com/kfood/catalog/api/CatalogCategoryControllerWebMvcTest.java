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

import com.kfood.catalog.app.CatalogCategoryAlreadyExistsException;
import com.kfood.catalog.app.CatalogCategoryNotFoundException;
import com.kfood.catalog.app.CreateCatalogCategoryUseCase;
import com.kfood.catalog.app.DeactivateCatalogCategoryUseCase;
import com.kfood.catalog.app.ListCatalogCategoriesUseCase;
import com.kfood.catalog.app.UpdateCatalogCategoryUseCase;
import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
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
class CatalogCategoryControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private CreateCatalogCategoryUseCase createCatalogCategoryUseCase;

  @MockitoBean private ListCatalogCategoriesUseCase listCatalogCategoriesUseCase;

  @MockitoBean private UpdateCatalogCategoryUseCase updateCatalogCategoryUseCase;

  @MockitoBean private DeactivateCatalogCategoryUseCase deactivateCatalogCategoryUseCase;

  @Test
  void shouldCreateCategorySuccessfully() throws Exception {
    var categoryId = UUID.randomUUID();
    when(createCatalogCategoryUseCase.execute(any(CreateCatalogCategoryRequest.class)))
        .thenReturn(new CatalogCategoryResponse(categoryId, "Pizzas", 10, true));

    mockMvc
        .perform(
            post("/v1/catalog/categories")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Pizzas",
                      "sortOrder": 10
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(categoryId.toString()))
        .andExpect(jsonPath("$.name").value("Pizzas"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void shouldListCategoriesForStore() throws Exception {
    when(listCatalogCategoriesUseCase.execute())
        .thenReturn(
            List.of(
                new CatalogCategoryResponse(UUID.randomUUID(), "Bebidas", 5, true),
                new CatalogCategoryResponse(UUID.randomUUID(), "Pizzas", 10, false)));

    mockMvc
        .perform(
            get("/v1/catalog/categories")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Bebidas"))
        .andExpect(jsonPath("$[1].name").value("Pizzas"));
  }

  @Test
  void shouldUpdateCategorySuccessfully() throws Exception {
    var categoryId = UUID.randomUUID();
    when(updateCatalogCategoryUseCase.execute(
            any(UUID.class), any(UpdateCatalogCategoryRequest.class)))
        .thenReturn(new CatalogCategoryResponse(categoryId, "Bebidas", 20, true));

    mockMvc
        .perform(
            put("/v1/catalog/categories/" + categoryId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Bebidas",
                      "sortOrder": 20
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Bebidas"))
        .andExpect(jsonPath("$.sortOrder").value(20));
  }

  @Test
  void shouldDeactivateCategorySuccessfully() throws Exception {
    var categoryId = UUID.randomUUID();
    when(deactivateCatalogCategoryUseCase.execute(categoryId))
        .thenReturn(new CatalogCategoryResponse(categoryId, "Pizzas", 10, false));

    mockMvc
        .perform(
            patch("/v1/catalog/categories/" + categoryId + "/inactive")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void shouldReturnConflictForDuplicateCategoryName() throws Exception {
    when(createCatalogCategoryUseCase.execute(any(CreateCatalogCategoryRequest.class)))
        .thenThrow(new CatalogCategoryAlreadyExistsException("Pizzas"));

    mockMvc
        .perform(
            post("/v1/catalog/categories")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Pizzas",
                      "sortOrder": 10
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnNotFoundWhenUpdatingMissingCategory() throws Exception {
    var categoryId = UUID.randomUUID();
    when(updateCatalogCategoryUseCase.execute(
            any(UUID.class), any(UpdateCatalogCategoryRequest.class)))
        .thenThrow(new CatalogCategoryNotFoundException(categoryId));

    mockMvc
        .perform(
            put("/v1/catalog/categories/" + categoryId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Bebidas",
                      "sortOrder": 20
                    }
                    """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldForbidAttendantFromCreatingCategory() throws Exception {
    mockMvc
        .perform(
            post("/v1/catalog/categories")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Pizzas",
                      "sortOrder": 10
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
