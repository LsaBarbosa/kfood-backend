package com.kfood.identity.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenReader;
import com.kfood.identity.app.LoginService;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.exceptions.GlobalExceptionHandler;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AuthControllerWebMvcTest.MockConfig.class})
class AuthControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private LoginService loginService;

  @Test
  @DisplayName("should return token when login is valid")
  void shouldReturnTokenWhenLoginIsValid() throws Exception {
    var response =
        new LoginResponse(
            "jwt-token",
            "Bearer",
            3600,
            new LoginResponse.AuthenticatedUserResponse(
                UUID.randomUUID(),
                "owner@kfood.local",
                Set.of("OWNER"),
                UUID.randomUUID(),
                "ACTIVE"));

    when(loginService.login(eq("owner@kfood.local"), eq("Senha@123"))).thenReturn(response);

    mockMvc
        .perform(
            post("/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "owner@kfood.local",
                      "password": "Senha@123"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("jwt-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600))
        .andExpect(jsonPath("$.user.email").value("owner@kfood.local"))
        .andExpect(jsonPath("$.user.roles[0]").value("OWNER"));
  }

  @Test
  @DisplayName("should return 401 when credentials are invalid")
  void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
    when(loginService.login(eq("owner@kfood.local"), eq("errada")))
        .thenThrow(
            new BusinessException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                "Invalid credentials.",
                HttpStatus.UNAUTHORIZED));

    mockMvc
        .perform(
            post("/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "owner@kfood.local",
                      "password": "errada"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.message").value("Invalid credentials."));
  }

  @TestConfiguration
  static class MockConfig {

    @Bean
    LoginService loginService() {
      return mock(LoginService.class);
    }

    @Bean
    JwtTokenReader jwtTokenReader() {
      return mock(JwtTokenReader.class);
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
      return mock(AuthenticationEntryPoint.class);
    }
  }
}
