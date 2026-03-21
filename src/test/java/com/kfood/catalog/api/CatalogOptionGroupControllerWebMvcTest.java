package com.kfood.catalog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.catalog.app.CatalogProductNotFoundException;
import com.kfood.catalog.app.CreateCatalogOptionGroupUseCase;
import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
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
class CatalogOptionGroupControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private CreateCatalogOptionGroupUseCase createCatalogOptionGroupUseCase;

  @Test
  void shouldCreateOptionGroupSuccessfully() throws Exception {
    var productId = UUID.randomUUID();
    var optionGroupId = UUID.randomUUID();
    when(createCatalogOptionGroupUseCase.execute(
            eq(productId), any(CreateCatalogOptionGroupRequest.class)))
        .thenReturn(
            new CatalogOptionGroupResponse(
                optionGroupId, productId, "Stuffed Crust", 0, 1, false, true));

    mockMvc
        .perform(
            post("/v1/catalog/products/" + productId + "/option-groups")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Stuffed Crust",
                      "minSelect": 0,
                      "maxSelect": 1,
                      "required": false,
                      "active": true
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(optionGroupId.toString()))
        .andExpect(jsonPath("$.productId").value(productId.toString()))
        .andExpect(jsonPath("$.name").value("Stuffed Crust"));
  }

  @Test
  void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
    var productId = UUID.randomUUID();
    when(createCatalogOptionGroupUseCase.execute(
            eq(productId), any(CreateCatalogOptionGroupRequest.class)))
        .thenThrow(new CatalogProductNotFoundException(productId));

    mockMvc
        .perform(
            post("/v1/catalog/products/" + productId + "/option-groups")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Sauces",
                      "minSelect": 0,
                      "maxSelect": 2,
                      "required": false,
                      "active": true
                    }
                    """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
    var productId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/v1/catalog/products/" + productId + "/option-groups")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": " ",
                      "minSelect": 0,
                      "maxSelect": 1,
                      "required": false,
                      "active": true
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("name"));
  }

  private String tokenOf(UserRoleName role) {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            role.name().toLowerCase() + "@kfood.local",
            "$2a$10$abcdefghijklmnopqrstuv",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(role));

    return jwtTokenService.generateToken(user);
  }
}
