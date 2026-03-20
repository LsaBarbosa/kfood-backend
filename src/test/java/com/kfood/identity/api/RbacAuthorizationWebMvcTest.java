package com.kfood.identity.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "app.security.jwt-secret=12345678901234567890123456789012",
      "app.security.jwt-expiration-seconds=3600"
    })
class RbacAuthorizationWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @Test
  @DisplayName("should allow admin to access admin route")
  void shouldAllowAdminToAccessAdminRoute() throws Exception {
    var token = tokenOf(UserRoleName.ADMIN);

    mockMvc
        .perform(
            get("/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resource").value("admin.audit-logs.read"));
  }

  @Test
  @DisplayName("should forbid owner to access admin route")
  void shouldForbidOwnerToAccessAdminRoute() throws Exception {
    var token = tokenOf(UserRoleName.OWNER);

    mockMvc
        .perform(
            get("/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  @DisplayName("should allow owner to accept terms")
  void shouldAllowOwnerToAcceptTerms() throws Exception {
    var token = tokenOf(UserRoleName.OWNER);

    mockMvc
        .perform(
            post("/v1/merchant/store/terms-acceptance")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resource").value("merchant.terms.accept"));
  }

  @Test
  @DisplayName("should forbid manager to accept terms")
  void shouldForbidManagerToAcceptTerms() throws Exception {
    var token = tokenOf(UserRoleName.MANAGER);

    mockMvc
        .perform(
            post("/v1/merchant/store/terms-acceptance")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  @DisplayName("should allow manager to create catalog category")
  void shouldAllowManagerToCreateCatalogCategory() throws Exception {
    var token = tokenOf(UserRoleName.MANAGER);

    mockMvc
        .perform(
            post("/v1/catalog/categories")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resource").value("catalog.category.create"));
  }

  @Test
  @DisplayName("should forbid attendant to create catalog category")
  void shouldForbidAttendantToCreateCatalogCategory() throws Exception {
    var token = tokenOf(UserRoleName.ATTENDANT);

    mockMvc
        .perform(
            post("/v1/catalog/categories")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  @DisplayName("should allow attendant to access orders route")
  void shouldAllowAttendantToAccessOrdersRoute() throws Exception {
    var token = tokenOf(UserRoleName.ATTENDANT);

    mockMvc
        .perform(
            get("/v1/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resource").value("orders.read"));
  }

  @Test
  @DisplayName("should forbid attendant to create merchant user")
  void shouldForbidAttendantToCreateMerchantUser() throws Exception {
    var token = tokenOf(UserRoleName.ATTENDANT);

    mockMvc
        .perform(
            post("/v1/merchant/users")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  @DisplayName("should allow owner to create store")
  void shouldAllowOwnerToCreateStore() throws Exception {
    var token = tokenOf(UserRoleName.OWNER);

    mockMvc
        .perform(
            post("/v1/merchant/store")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resource").value("merchant.store.create"));
  }

  @Test
  @DisplayName("should allow admin to create store")
  void shouldAllowAdminToCreateStore() throws Exception {
    var token = tokenOf(UserRoleName.ADMIN);

    mockMvc
        .perform(
            post("/v1/merchant/store")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resource").value("merchant.store.create"));
  }

  @Test
  @DisplayName("should forbid attendant to create store")
  void shouldForbidAttendantToCreateStore() throws Exception {
    var token = tokenOf(UserRoleName.ATTENDANT);

    mockMvc
        .perform(
            post("/v1/merchant/store")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
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
