package com.kfood.merchant.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.merchant.app.GetStoreHoursUseCase;
import com.kfood.merchant.app.InvalidStoreHoursException;
import com.kfood.merchant.app.UpdateStoreHoursUseCase;
import java.time.DayOfWeek;
import java.time.LocalTime;
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
class StoreHoursControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private UpdateStoreHoursUseCase updateStoreHoursUseCase;

  @MockitoBean private GetStoreHoursUseCase getStoreHoursUseCase;

  @Test
  void shouldUpdateStoreHours() throws Exception {
    when(updateStoreHoursUseCase.execute(any(UpdateStoreHoursRequest.class)))
        .thenReturn(new UpdateStoreHoursResponse(true, 1));

    mockMvc
        .perform(
            put("/v1/merchant/store/hours")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "hours": [
                        {
                          "dayOfWeek": "MONDAY",
                          "openTime": "10:00:00",
                          "closeTime": "22:00:00",
                          "isClosed": false
                        },
                        {
                          "dayOfWeek": "SUNDAY",
                          "isClosed": true
                        }
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated").value(true))
        .andExpect(jsonPath("$.hoursVersion").value(1));
  }

  @Test
  void shouldReturnBadRequestWhenBusinessRuleFails() throws Exception {
    when(updateStoreHoursUseCase.execute(any(UpdateStoreHoursRequest.class)))
        .thenThrow(new InvalidStoreHoursException("openTime must be before closeTime"));

    mockMvc
        .perform(
            put("/v1/merchant/store/hours")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "hours": [
                        {
                          "dayOfWeek": "MONDAY",
                          "openTime": "22:00:00",
                          "closeTime": "10:00:00",
                          "isClosed": false
                        }
                      ]
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldAllowConsultationForAttendant() throws Exception {
    when(getStoreHoursUseCase.execute())
        .thenReturn(
            new StoreHoursResponse(
                1,
                List.of(
                    new StoreHourResponse(
                        DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false))));

    mockMvc
        .perform(
            get("/v1/merchant/store/hours")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hoursVersion").value(1))
        .andExpect(jsonPath("$.hours[0].dayOfWeek").value("MONDAY"));
  }

  @Test
  void shouldBlockUpdateForAttendant() throws Exception {
    mockMvc
        .perform(
            put("/v1/merchant/store/hours")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "hours": [
                        {
                          "dayOfWeek": "MONDAY",
                          "openTime": "10:00:00",
                          "closeTime": "22:00:00",
                          "isClosed": false
                        }
                      ]
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  void shouldRequireAuthentication() throws Exception {
    mockMvc.perform(get("/v1/merchant/store/hours")).andExpect(status().isUnauthorized());
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
