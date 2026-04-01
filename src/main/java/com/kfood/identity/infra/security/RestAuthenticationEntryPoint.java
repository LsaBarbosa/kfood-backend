package com.kfood.identity.infra.security;

import com.kfood.shared.exceptions.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ApiErrorResponseFactory apiErrorResponseFactory;

  public RestAuthenticationEntryPoint(ApiErrorResponseFactory apiErrorResponseFactory) {
    this.apiErrorResponseFactory = apiErrorResponseFactory;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    apiErrorResponseFactory.write(
        response,
        HttpStatus.UNAUTHORIZED,
        apiErrorResponseFactory.createAuthenticationError(request, authException));
  }
}
