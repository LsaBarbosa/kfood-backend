package com.kfood.merchant.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.infra.security.JwtAuthenticationFilter;
import com.kfood.merchant.app.AdminChangeStoreStatusUseCase;
import com.kfood.merchant.app.ChangeStoreStatusCommand;
import com.kfood.merchant.app.StoreDetailsOutput;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.config.SecurityConfiguration;
import com.kfood.shared.exceptions.ApiErrorResponseFactory;
import com.kfood.shared.exceptions.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AdminStoreStatusController.class,
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = SecurityConfiguration.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = JwtAuthenticationFilter.class)
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({
  GlobalExceptionHandler.class,
  ApiErrorResponseFactory.class,
  AdminStoreStatusControllerWebMvcTest.TestSecurityConfig.class
})
class AdminStoreStatusControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AdminChangeStoreStatusUseCase adminChangeStoreStatusUseCase;

  @Test
  @WithMockUser(username = "admin@kfood.local", roles = "ADMIN")
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
  @WithMockUser(username = "owner@kfood.local", roles = "OWNER")
  void shouldForbidOwnerFromUsingAdminStoreStatusRoute() throws Exception {
    mockMvc
        .perform(
            patch("/v1/admin/stores/{storeId}/status", UUID.randomUUID())
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
  @WithMockUser(username = "manager@kfood.local", roles = "MANAGER")
  void shouldForbidManagerFromUsingAdminStoreStatusRoute() throws Exception {
    mockMvc
        .perform(
            patch("/v1/admin/stores/{storeId}/status", UUID.randomUUID())
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
  @WithMockUser(username = "attendant@kfood.local", roles = "ATTENDANT")
  void shouldForbidAttendantFromUsingAdminStoreStatusRoute() throws Exception {
    mockMvc
        .perform(
            patch("/v1/admin/stores/{storeId}/status", UUID.randomUUID())
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

  @TestConfiguration
  @EnableMethodSecurity
  static class TestSecurityConfig {}
}
