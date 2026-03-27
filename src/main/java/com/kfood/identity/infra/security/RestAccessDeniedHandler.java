package com.kfood.identity.infra.security;

import com.kfood.shared.exceptions.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ApiErrorResponseFactory apiErrorResponseFactory;

  public RestAccessDeniedHandler(ApiErrorResponseFactory apiErrorResponseFactory) {
    this.apiErrorResponseFactory = apiErrorResponseFactory;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    apiErrorResponseFactory.write(
        response,
        HttpStatus.FORBIDDEN,
        apiErrorResponseFactory.createAccessDeniedError(request, accessDeniedException));
  }
}
