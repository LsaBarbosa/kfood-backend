package com.kfood.merchant.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.merchant.app.AdminChangeStoreStatusUseCase;
import com.kfood.merchant.app.ChangeStoreStatusCommand;
import com.kfood.merchant.app.StoreDetailsOutput;
import com.kfood.merchant.domain.StoreStatus;
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
class AdminStoreStatusControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private AdminChangeStoreStatusUseCase adminChangeStoreStatusUseCase;

  @Test
  void shouldAllowAdminToChangeStatusForExplicitTargetStore() throws Exception {
    var targetStoreId = UUID.randomUUID();
    when(adminChangeStoreStatusUseCase.execute(
            targetStoreId, new ChangeStoreStatusCommand(StoreStatus.SUSPENDED)))
        .thenReturn(
            new StoreDetailsOutput(
                targetStoreId,
                "loja-do-bairro",
                "Loja do Bairro",
                StoreStatus.SUSPENDED,
                "21999990000",
                "America/Sao_Paulo",
                true,
                true));

    mockMvc
        .perform(
            patch("/v1/admin/stores/{storeId}/status", targetStoreId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ADMIN))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetStatus": "SUSPENDED"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(targetStoreId.toString()))
        .andExpect(jsonPath("$.slug").value("loja-do-bairro"))
        .andExpect(jsonPath("$.name").value("Loja do Bairro"))
        .andExpect(jsonPath("$.status").value("SUSPENDED"))
        .andExpect(jsonPath("$.phone").value("21999990000"))
        .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
        .andExpect(jsonPath("$.hoursConfigured").value(true))
        .andExpect(jsonPath("$.deliveryZonesConfigured").value(true));

    verify(adminChangeStoreStatusUseCase)
        .execute(targetStoreId, new ChangeStoreStatusCommand(StoreStatus.SUSPENDED));
  }

  @Test
  void shouldForbidOwnerFromUsingAdminStoreStatusRoute() throws Exception {
    mockMvc
        .perform(
            patch("/v1/admin/stores/{storeId}/status", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetStatus": "ACTIVE"
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  void shouldForbidManagerFromUsingAdminStoreStatusRoute() throws Exception {
    mockMvc
        .perform(
            patch("/v1/admin/stores/{storeId}/status", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetStatus": "ACTIVE"
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  void shouldForbidAttendantFromUsingAdminStoreStatusRoute() throws Exception {
    mockMvc
        .perform(
            patch("/v1/admin/stores/{storeId}/status", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetStatus": "ACTIVE"
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  private String tokenOf(UserRoleName roleName) {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            null,
            roleName.name().toLowerCase() + "@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(roleName));
    return jwtTokenService.generateToken(user);
  }
}
