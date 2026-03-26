package com.kfood.identity.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.identity.app.JwtTokenReader;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.mockito.ArgumentCaptor;

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

    ArgumentCaptor<org.springframework.security.core.AuthenticationException> captor =
        ArgumentCaptor.forClass(org.springframework.security.core.AuthenticationException.class);
    verify(authenticationEntryPoint).commence(same(request), same(response), captor.capture());
    verify(chain, never()).doFilter(any(), any());
    assertThat(captor.getValue()).isInstanceOf(BadCredentialsException.class);
    assertThat(captor.getValue().getMessage()).isEqualTo("Invalid authorization header.");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("should reject expired token with credentials expired exception")
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

    ArgumentCaptor<org.springframework.security.core.AuthenticationException> captor =
        ArgumentCaptor.forClass(org.springframework.security.core.AuthenticationException.class);
    verify(authenticationEntryPoint).commence(same(request), same(response), captor.capture());
    verify(chain, never()).doFilter(any(), any());
    assertThat(captor.getValue()).isInstanceOf(CredentialsExpiredException.class);
    assertThat(captor.getValue().getCause()).isSameAs(expiredException);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldRejectInvalidToken() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid");
    var response = new MockHttpServletResponse();
    var chain = mock(jakarta.servlet.FilterChain.class);
    when(jwtTokenReader.read("invalid")).thenThrow(new IllegalArgumentException("bad token"));

    filter.doFilter(request, response, chain);

    ArgumentCaptor<org.springframework.security.core.AuthenticationException> captor =
        ArgumentCaptor.forClass(org.springframework.security.core.AuthenticationException.class);
    verify(authenticationEntryPoint).commence(same(request), same(response), captor.capture());
    verify(chain, never()).doFilter(any(), any());
    assertThat(captor.getValue()).isInstanceOf(BadCredentialsException.class);
    assertThat(captor.getValue().getMessage()).isEqualTo("Invalid token.");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldRejectJwtExceptionToken() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer malformed");
    var response = new MockHttpServletResponse();
    var chain = mock(jakarta.servlet.FilterChain.class);
    when(jwtTokenReader.read("malformed")).thenThrow(new JwtException("bad jwt"));

    filter.doFilter(request, response, chain);

    ArgumentCaptor<org.springframework.security.core.AuthenticationException> captor =
        ArgumentCaptor.forClass(org.springframework.security.core.AuthenticationException.class);
    verify(authenticationEntryPoint).commence(same(request), same(response), captor.capture());
    verify(chain, never()).doFilter(any(), any());
    assertThat(captor.getValue()).isInstanceOf(BadCredentialsException.class);
    assertThat(captor.getValue().getMessage()).isEqualTo("Invalid token.");
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
