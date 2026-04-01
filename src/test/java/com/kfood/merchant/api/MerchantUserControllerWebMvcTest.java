package com.kfood.merchant.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.merchant.application.user.MerchantUserOutput;
import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import java.time.Instant;
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
class MerchantUserControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private MerchantUserManagementPort merchantUserManagementPort;
  @MockitoBean private MerchantTenantAccessPort merchantTenantAccessPort;

  @Test
  void shouldCreateMerchantUserWithValidPayload() throws Exception {
    when(merchantTenantAccessPort.getRequiredStoreId()).thenReturn(UUID.randomUUID());
    when(merchantUserManagementPort.create(
            any(UUID.class), any(String.class), any(String.class), any(Set.class)))
        .thenReturn(
            new MerchantUserOutput(
                UUID.randomUUID(),
                "manager@kfood.local",
                List.of("MANAGER"),
                UserStatus.ACTIVE,
                Instant.parse("2026-03-26T12:00:00Z")));

    mockMvc
        .perform(
            post("/v1/merchant/users")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "manager@kfood.local",
                      "password": "Senha@123",
                      "roles": ["MANAGER"]
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("manager@kfood.local"))
        .andExpect(jsonPath("$.roles[0]").value("MANAGER"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void shouldListMerchantUsers() throws Exception {
    when(merchantTenantAccessPort.getRequiredStoreId()).thenReturn(UUID.randomUUID());
    when(merchantUserManagementPort.listByStoreId(any(UUID.class)))
        .thenReturn(
            List.of(
                new MerchantUserOutput(
                    UUID.randomUUID(),
                    "attendant@kfood.local",
                    List.of("ATTENDANT"),
                    UserStatus.ACTIVE,
                    Instant.parse("2026-03-26T12:00:00Z"))));

    mockMvc
        .perform(
            get("/v1/merchant/users")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value("attendant@kfood.local"))
        .andExpect(jsonPath("$[0].roles[0]").value("ATTENDANT"))
        .andExpect(jsonPath("$[0].password").doesNotExist())
        .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
  }

  @Test
  void shouldReturnBadRequestForInvalidPayload() throws Exception {
    mockMvc
        .perform(
            post("/v1/merchant/users")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "email-invalido",
                      "password": "123",
                      "roles": []
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldRequireAuthentication() throws Exception {
    mockMvc.perform(get("/v1/merchant/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldForbidRoleWithInsufficientPrivileges() throws Exception {
    mockMvc
        .perform(
            post("/v1/merchant/users")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "manager@kfood.local",
                      "password": "Senha@123",
                      "roles": ["MANAGER"]
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
