package com.kfood.merchant.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.merchant.app.ChangeStoreStatusUseCase;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceUseCase;
import com.kfood.merchant.app.CreateStoreUseCase;
import com.kfood.merchant.app.GetStoreDetailsUseCase;
import com.kfood.merchant.app.StoreActivationRequirementsNotMetException;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.UpdateStoreUseCase;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreStatus;
import java.time.Instant;
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
class StoreControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private CreateStoreUseCase createStoreUseCase;

  @MockitoBean private UpdateStoreUseCase updateStoreUseCase;

  @MockitoBean private GetStoreDetailsUseCase getStoreDetailsUseCase;

  @MockitoBean private CreateStoreTermsAcceptanceUseCase createStoreTermsAcceptanceUseCase;

  @MockitoBean private ChangeStoreStatusUseCase changeStoreStatusUseCase;

  @Test
  void shouldCreateStoreSuccessfully() throws Exception {
    when(createStoreUseCase.execute(any(CreateStoreRequest.class)))
        .thenReturn(
            new CreateStoreResponse(
                UUID.randomUUID(),
                "loja-do-bairro",
                StoreStatus.SETUP,
                Instant.parse("2026-03-20T10:00:00Z")));

    mockMvc
        .perform(
            post("/v1/merchant/store")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER, null))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Loja do Bairro",
                      "slug": "loja-do-bairro",
                      "cnpj": "45.723.174/0001-10",
                      "phone": "21999990000",
                      "timezone": "America/Sao_Paulo"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.slug").value("loja-do-bairro"))
        .andExpect(jsonPath("$.status").value("SETUP"));
  }

  @Test
  void shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
    mockMvc
        .perform(
            put("/v1/merchant/store")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "slug": "Slug Invalido"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnNotFoundWhenStoreDoesNotExist() throws Exception {
    var storeId = UUID.randomUUID();
    when(updateStoreUseCase.execute(any(UpdateStoreRequest.class)))
        .thenThrow(new StoreNotFoundException(storeId));

    mockMvc
        .perform(
            put("/v1/merchant/store")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Novo nome"
                    }
                    """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldUpdateStoreSuccessfully() throws Exception {
    var storeId = UUID.randomUUID();
    when(updateStoreUseCase.execute(any(UpdateStoreRequest.class)))
        .thenReturn(
            new StoreResponse(
                storeId,
                "Loja Premium",
                "loja-premium",
                "45.723.174/0001-10",
                "21911112222",
                "UTC",
                StoreStatus.SETUP));

    mockMvc
        .perform(
            put("/v1/merchant/store")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Loja Premium",
                      "slug": "loja-premium"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Loja Premium"))
        .andExpect(jsonPath("$.slug").value("loja-premium"));
  }

  @Test
  void shouldBlockRoleWithoutPermission() throws Exception {
    mockMvc
        .perform(
            put("/v1/merchant/store")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Novo nome"
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  void shouldRequireAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/v1/merchant/store")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Loja do Bairro",
                      "slug": "loja-do-bairro",
                      "cnpj": "45.723.174/0001-10",
                      "phone": "21999990000",
                      "timezone": "America/Sao_Paulo"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void shouldGetCurrentStoreDetails() throws Exception {
    when(getStoreDetailsUseCase.execute())
        .thenReturn(
            new StoreDetailsResponse(
                UUID.randomUUID(),
                "loja-do-bairro",
                "Loja do Bairro",
                StoreStatus.SETUP,
                "21999990000",
                "America/Sao_Paulo",
                true,
                false));

    mockMvc
        .perform(
            get("/v1/merchant/store")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SETUP"))
        .andExpect(jsonPath("$.hoursConfigured").value(true))
        .andExpect(jsonPath("$.deliveryZonesConfigured").value(false));
  }

  @Test
  void shouldChangeStoreStatus() throws Exception {
    when(changeStoreStatusUseCase.execute(any(ChangeStoreStatusRequest.class)))
        .thenReturn(
            new StoreDetailsResponse(
                UUID.randomUUID(),
                "loja-do-bairro",
                "Loja do Bairro",
                StoreStatus.ACTIVE,
                "21999990000",
                "America/Sao_Paulo",
                true,
                true));

    mockMvc
        .perform(
            patch("/v1/merchant/store/status")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetStatus": "ACTIVE"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void shouldReturnConflictWhenActivationRequirementsAreMissing() throws Exception {
    when(changeStoreStatusUseCase.execute(any(ChangeStoreStatusRequest.class)))
        .thenThrow(
            new StoreActivationRequirementsNotMetException(java.util.List.of("termsAccepted")));

    mockMvc
        .perform(
            patch("/v1/merchant/store/status")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetStatus": "ACTIVE"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STORE_NOT_ACTIVE"))
        .andExpect(jsonPath("$.details[0].field").value("termsAccepted"));
  }

  @Test
  void shouldBlockStatusChangeForManager() throws Exception {
    mockMvc
        .perform(
            patch("/v1/merchant/store/status")
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
  void shouldAllowStatusChangeForAdmin() throws Exception {
    when(changeStoreStatusUseCase.execute(any(ChangeStoreStatusRequest.class)))
        .thenReturn(
            new StoreDetailsResponse(
                UUID.randomUUID(),
                "loja-do-bairro",
                "Loja do Bairro",
                StoreStatus.SUSPENDED,
                "21999990000",
                "America/Sao_Paulo",
                true,
                true));

    mockMvc
        .perform(
            patch("/v1/merchant/store/status")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ADMIN))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "targetStatus": "SUSPENDED"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUSPENDED"));
  }

  @Test
  void shouldAcceptTermsSuccessfully() throws Exception {
    when(createStoreTermsAcceptanceUseCase.execute(any(CreateStoreTermsAcceptanceRequest.class)))
        .thenReturn(
            new StoreTermsAcceptanceResponse(
                UUID.randomUUID(),
                LegalDocumentType.TERMS_OF_USE,
                "2026.03",
                Instant.parse("2026-03-20T10:15:00Z")));

    mockMvc
        .perform(
            post("/v1/merchant/store/terms-acceptance")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "documentType": "TERMS_OF_USE",
                      "documentVersion": "2026.03",
                      "acceptedAt": "2026-03-20T10:15:00Z"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.documentType").value("TERMS_OF_USE"))
        .andExpect(jsonPath("$.documentVersion").value("2026.03"));
  }

  @Test
  void shouldForbidManagerFromAcceptingTerms() throws Exception {
    mockMvc
        .perform(
            post("/v1/merchant/store/terms-acceptance")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "documentType": "TERMS_OF_USE",
                      "documentVersion": "2026.03",
                      "acceptedAt": "2026-03-20T10:15:00Z"
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  void shouldReturnBadRequestWhenTermsAcceptancePayloadIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/v1/merchant/store/terms-acceptance")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "documentVersion": "",
                      "acceptedAt": null
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnConflictWhenStoreIsSuspendedForCriticalOperation() throws Exception {
    when(updateStoreUseCase.execute(any(UpdateStoreRequest.class)))
        .thenThrow(new StoreNotActiveException(UUID.randomUUID(), StoreStatus.SUSPENDED));

    mockMvc
        .perform(
            put("/v1/merchant/store")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Novo nome"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STORE_NOT_ACTIVE"));
  }

  private String tokenOf(UserRoleName role) {
    return tokenOf(role, UUID.randomUUID());
  }

  private String tokenOf(UserRoleName role, UUID storeId) {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            storeId,
            role.name().toLowerCase() + "@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(role));
    return jwtTokenService.generateToken(user);
  }
}
