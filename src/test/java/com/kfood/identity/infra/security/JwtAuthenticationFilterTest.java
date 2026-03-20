package com.kfood.identity.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.identity.app.JwtTokenReader;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

class JwtAuthenticationFilterTest {

  private final JwtTokenReader jwtTokenReader = mock(JwtTokenReader.class);
  private final AuthenticationEntryPoint authenticationEntryPoint =
      mock(AuthenticationEntryPoint.class);
  private final JwtAuthenticationFilter filter =
      new JwtAuthenticationFilter(jwtTokenReader, authenticationEntryPoint);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSkipWhenAuthorizationHeaderIsMissing() throws Exception {
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var chain = mock(jakarta.servlet.FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(authenticationEntryPoint, never()).commence(any(), any(), any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldSkipWhenAuthorizationHeaderIsBlank() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "   ");
    var response = new MockHttpServletResponse();
    var chain = mock(jakarta.servlet.FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(authenticationEntryPoint, never()).commence(any(), any(), any());
  }

  @Test
  void shouldRejectInvalidAuthorizationHeader() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Token abc");
    var response = new MockHttpServletResponse();
    var chain = mock(jakarta.servlet.FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(authenticationEntryPoint).commence(any(), any(), any());
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void shouldRejectExpiredToken() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer expired");
    var response = new MockHttpServletResponse();
    var chain = mock(jakarta.servlet.FilterChain.class);
    var expiredException =
        new ExpiredJwtException(
            null, Jwts.claims().subject("owner@kfood.local").build(), "Token expired");
    when(jwtTokenReader.read("expired")).thenThrow(expiredException);

    filter.doFilter(request, response, chain);

    verify(authenticationEntryPoint).commence(any(), any(), any());
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void shouldRejectInvalidToken() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid");
    var response = new MockHttpServletResponse();
    var chain = mock(jakarta.servlet.FilterChain.class);
    when(jwtTokenReader.read("invalid")).thenThrow(new IllegalArgumentException("bad token"));

    filter.doFilter(request, response, chain);

    verify(authenticationEntryPoint).commence(any(), any(), any());
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void shouldAuthenticateAndContinueWhenTokenIsValid() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid");
    var response = new MockHttpServletResponse();
    var chain = mock(jakarta.servlet.FilterChain.class);
    var principal =
        new JwtTokenReader.AuthenticatedPrincipal(
            UUID.randomUUID(), "owner@kfood.local", UUID.randomUUID(), List.of("OWNER"));
    when(jwtTokenReader.read("valid")).thenReturn(principal);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(authenticationEntryPoint, never()).commence(any(), any(), any());
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .isInstanceOf(JwtAuthenticationToken.class);
    var authentication =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication.getPrincipal().getUsername()).isEqualTo("owner@kfood.local");
    assertThat(authentication.getPrincipal().getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_OWNER");
    assertThat(authentication.getDetails()).isNotNull();
  }
}
