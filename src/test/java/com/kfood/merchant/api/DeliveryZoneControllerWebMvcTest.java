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
import com.kfood.merchant.app.CreateDeliveryZoneCommand;
import com.kfood.merchant.app.CreateDeliveryZoneUseCase;
import com.kfood.merchant.app.DeliveryZoneAlreadyExistsException;
import com.kfood.merchant.app.DeliveryZoneOutput;
import com.kfood.merchant.app.GetDeliveryZoneUseCase;
import com.kfood.merchant.app.ListDeliveryZonesUseCase;
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
class DeliveryZoneControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private CreateDeliveryZoneUseCase createDeliveryZoneUseCase;

  @MockitoBean private GetDeliveryZoneUseCase getDeliveryZoneUseCase;

  @MockitoBean private ListDeliveryZonesUseCase listDeliveryZonesUseCase;

  @Test
  void shouldCreateZoneSuccessfully() throws Exception {
    var zoneId = UUID.randomUUID();
    when(createDeliveryZoneUseCase.execute(any(CreateDeliveryZoneCommand.class)))
        .thenReturn(
            new DeliveryZoneOutput(
                zoneId, "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true));

    mockMvc
        .perform(
            post("/v1/merchant/store/zones")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "zoneName": "Centro",
                      "feeAmount": 6.50,
                      "minOrderAmount": 25.00,
                      "active": true
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(zoneId.toString()))
        .andExpect(jsonPath("$.zoneName").value("Centro"));
  }

  @Test
  void shouldReturnBadRequestWhenFeeIsNegative() throws Exception {
    mockMvc
        .perform(
            post("/v1/merchant/store/zones")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "zoneName": "Centro",
                      "feeAmount": -1.00,
                      "minOrderAmount": 25.00,
                      "active": true
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnConflictForDuplicateZone() throws Exception {
    when(createDeliveryZoneUseCase.execute(any(CreateDeliveryZoneCommand.class)))
        .thenThrow(new DeliveryZoneAlreadyExistsException("Centro"));

    mockMvc
        .perform(
            post("/v1/merchant/store/zones")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "zoneName": "Centro",
                      "feeAmount": 6.50,
                      "minOrderAmount": 25.00,
                      "active": true
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldAllowGetZoneForAttendant() throws Exception {
    var zoneId = UUID.randomUUID();
    when(getDeliveryZoneUseCase.execute(zoneId))
        .thenReturn(
            new DeliveryZoneOutput(
                zoneId, "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true));

    mockMvc
        .perform(
            get("/v1/merchant/store/zones/" + zoneId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.feeAmount").value(6.5));
  }

  @Test
  void shouldAllowListZonesForAttendant() throws Exception {
    when(listDeliveryZonesUseCase.execute())
        .thenReturn(
            List.of(
                new DeliveryZoneOutput(
                    UUID.randomUUID(),
                    "Centro",
                    new BigDecimal("6.50"),
                    new BigDecimal("25.00"),
                    true)));

    mockMvc
        .perform(
            get("/v1/merchant/store/zones")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].zoneName").value("Centro"));
  }

  @Test
  void shouldBlockCreateForAttendant() throws Exception {
    mockMvc
        .perform(
            post("/v1/merchant/store/zones")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "zoneName": "Centro",
                      "feeAmount": 6.50,
                      "minOrderAmount": 25.00,
                      "active": true
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  void shouldRequireAuthentication() throws Exception {
    mockMvc.perform(get("/v1/merchant/store/zones")).andExpect(status().isUnauthorized());
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
