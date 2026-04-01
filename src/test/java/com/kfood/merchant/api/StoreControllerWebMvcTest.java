package com.kfood.merchant.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.kfood.merchant.app.ChangeStoreStatusCommand;
import com.kfood.merchant.app.ChangeStoreStatusUseCase;
import com.kfood.merchant.app.CreateStoreCommand;
import com.kfood.merchant.app.CreateStoreOutput;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceCommand;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceUseCase;
import com.kfood.merchant.app.CreateStoreUseCase;
import com.kfood.merchant.app.GetStoreDetailsUseCase;
import com.kfood.merchant.app.GetStoreTermsAcceptanceHistoryUseCase;
import com.kfood.merchant.app.StoreActivationRequirementsNotMetException;
import com.kfood.merchant.app.StoreDetailsOutput;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOutput;
import com.kfood.merchant.app.StoreTermsAcceptanceHistoryItemOutput;
import com.kfood.merchant.app.StoreTermsAcceptanceOutput;
import com.kfood.merchant.app.TenantAccessDeniedException;
import com.kfood.merchant.app.UpdateStoreCommand;
import com.kfood.merchant.app.UpdateStoreUseCase;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.web.ClientIpResolver;
import java.lang.reflect.RecordComponent;
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
class StoreControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private CreateStoreUseCase createStoreUseCase;

  @MockitoBean private UpdateStoreUseCase updateStoreUseCase;

  @MockitoBean private GetStoreDetailsUseCase getStoreDetailsUseCase;

  @MockitoBean private CreateStoreTermsAcceptanceUseCase createStoreTermsAcceptanceUseCase;

  @MockitoBean private GetStoreTermsAcceptanceHistoryUseCase getStoreTermsAcceptanceHistoryUseCase;

  @MockitoBean private ChangeStoreStatusUseCase changeStoreStatusUseCase;

  @MockitoBean private ClientIpResolver clientIpResolver;

  @Test
  void shouldExposeOnlyDocumentTypeAndDocumentVersionInTermsAcceptanceRequest() {
    assertThat(
            java.util.Arrays.stream(CreateStoreTermsAcceptanceRequest.class.getRecordComponents())
                .map(RecordComponent::getName))
        .containsExactly("documentType", "documentVersion")
        .doesNotContain("acceptedAt");
  }

  @Test
  void shouldCreateStoreSuccessfully() throws Exception {
    when(createStoreUseCase.execute(any(CreateStoreCommand.class)))
        .thenReturn(
            new CreateStoreOutput(
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
    when(updateStoreUseCase.execute(any(UpdateStoreCommand.class)))
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
    when(updateStoreUseCase.execute(any(UpdateStoreCommand.class)))
        .thenReturn(
            new StoreOutput(
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
            new StoreDetailsOutput(
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
    var storeId = UUID.randomUUID();
    when(changeStoreStatusUseCase.execute(any(ChangeStoreStatusCommand.class)))
        .thenReturn(
            new StoreDetailsOutput(
                storeId,
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
        .andExpect(jsonPath("$.id").value(storeId.toString()))
        .andExpect(jsonPath("$.slug").value("loja-do-bairro"))
        .andExpect(jsonPath("$.name").value("Loja do Bairro"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.phone").value("21999990000"))
        .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
        .andExpect(jsonPath("$.hoursConfigured").value(true))
        .andExpect(jsonPath("$.deliveryZonesConfigured").value(true));
  }

  @Test
  void shouldReturnConflictWhenActivationRequirementsAreMissing() throws Exception {
    when(changeStoreStatusUseCase.execute(any(ChangeStoreStatusCommand.class)))
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
    when(changeStoreStatusUseCase.execute(any(ChangeStoreStatusCommand.class)))
        .thenReturn(
            new StoreDetailsOutput(
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
  void shouldAcceptTermsSuccessfullyWithoutAcceptedAtInRequest() throws Exception {
    when(clientIpResolver.resolve(any())).thenReturn("203.0.113.9");
    when(createStoreTermsAcceptanceUseCase.execute(
            any(CreateStoreTermsAcceptanceCommand.class), any(String.class)))
        .thenReturn(
            new StoreTermsAcceptanceOutput(
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
                      "documentVersion": "2026.03"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.documentType").value("TERMS_OF_USE"))
        .andExpect(jsonPath("$.documentVersion").value("2026.03"))
        .andExpect(jsonPath("$.acceptedAt").value("2026-03-20T10:15:00Z"));
  }

  @Test
  void shouldRequireAuthenticationToAcceptTerms() throws Exception {
    mockMvc
        .perform(
            post("/v1/merchant/store/terms-acceptance")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "documentType": "TERMS_OF_USE",
                      "documentVersion": "2026.03"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void shouldReturnTenantAccessDeniedWhenAcceptingTermsAcrossTenants() throws Exception {
    when(clientIpResolver.resolve(any())).thenReturn("203.0.113.9");
    when(createStoreTermsAcceptanceUseCase.execute(
            any(CreateStoreTermsAcceptanceCommand.class), any(String.class)))
        .thenThrow(new TenantAccessDeniedException());

    mockMvc
        .perform(
            post("/v1/merchant/store/terms-acceptance")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "documentType": "TERMS_OF_USE",
                      "documentVersion": "2026.03"
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"))
        .andExpect(
            jsonPath("$.message").value("Authenticated user cannot operate on another tenant."));
  }

  @Test
  void shouldGetTermsAcceptanceHistorySuccessfully() throws Exception {
    var firstAcceptanceId = UUID.randomUUID();
    var secondAcceptanceId = UUID.randomUUID();
    var firstAcceptedByUserId = UUID.randomUUID();
    var secondAcceptedByUserId = UUID.randomUUID();
    when(getStoreTermsAcceptanceHistoryUseCase.execute())
        .thenReturn(
            List.of(
                new StoreTermsAcceptanceHistoryItemOutput(
                    firstAcceptanceId,
                    firstAcceptedByUserId,
                    LegalDocumentType.TERMS_OF_USE,
                    "2026.04",
                    Instant.parse("2026-04-20T13:15:00Z")),
                new StoreTermsAcceptanceHistoryItemOutput(
                    secondAcceptanceId,
                    secondAcceptedByUserId,
                    LegalDocumentType.TERMS_OF_USE,
                    "2026.03",
                    Instant.parse("2026-03-20T13:15:00Z"))));

    mockMvc
        .perform(
            get("/v1/merchant/store/terms-acceptance/history")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(firstAcceptanceId.toString()))
        .andExpect(jsonPath("$[0].acceptedByUserId").value(firstAcceptedByUserId.toString()))
        .andExpect(jsonPath("$[0].documentType").value("TERMS_OF_USE"))
        .andExpect(jsonPath("$[0].documentVersion").value("2026.04"))
        .andExpect(jsonPath("$[0].acceptedAt").value("2026-04-20T13:15:00Z"))
        .andExpect(jsonPath("$[1].id").value(secondAcceptanceId.toString()))
        .andExpect(jsonPath("$[1].acceptedByUserId").value(secondAcceptedByUserId.toString()))
        .andExpect(jsonPath("$[1].documentVersion").value("2026.03"))
        .andExpect(jsonPath("$[1].acceptedAt").value("2026-03-20T13:15:00Z"));
  }

  @Test
  void shouldRequireAuthenticationToGetTermsAcceptanceHistory() throws Exception {
    mockMvc
        .perform(get("/v1/merchant/store/terms-acceptance/history"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void shouldForbidManagerFromGettingTermsAcceptanceHistory() throws Exception {
    mockMvc
        .perform(
            get("/v1/merchant/store/terms-acceptance/history")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  void shouldReturnNotFoundWhenGettingTermsAcceptanceHistoryForMissingStore() throws Exception {
    var storeId = UUID.randomUUID();
    when(getStoreTermsAcceptanceHistoryUseCase.execute())
        .thenThrow(new StoreNotFoundException(storeId));

    mockMvc
        .perform(
            get("/v1/merchant/store/terms-acceptance/history")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
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
                      "documentVersion": "2026.03"
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
                      "documentType": null
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnConflictWhenStoreIsSuspendedForCriticalOperation() throws Exception {
    when(updateStoreUseCase.execute(any(UpdateStoreCommand.class)))
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
