package com.kfood.identity.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class SecureRouteProtectionWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @Test
  @DisplayName("should return 401 when route is accessed without token")
  void shouldReturn401WhenRouteIsAccessedWithoutToken() throws Exception {
    mockMvc
        .perform(get("/v1/merchant/me").contentType(APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.message").value("Authentication is required or token is invalid."))
        .andExpect(jsonPath("$.path").value("/v1/merchant/me"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.details").isEmpty())
        .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  @DisplayName("should return 401 when token is invalid")
  void shouldReturn401WhenTokenIsInvalid() throws Exception {
    mockMvc
        .perform(
            get("/v1/merchant/me")
                .header("Authorization", "Bearer token-invalido")
                .contentType(APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.message").value("Authentication is required or token is invalid."))
        .andExpect(jsonPath("$.path").value("/v1/merchant/me"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.details").isEmpty())
        .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  @DisplayName("should allow access when token is valid")
  void shouldAllowAccessWhenTokenIsValid() throws Exception {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "owner@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));

    var token = jwtTokenService.generateToken(user);

    mockMvc
        .perform(
            get("/v1/merchant/me")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("owner@kfood.local"))
        .andExpect(jsonPath("$.roles[0]").value("ROLE_OWNER"));
  }
}
