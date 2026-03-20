package com.kfood.identity.infra.security;

import com.kfood.identity.app.AuthenticatedUser;
import com.kfood.identity.app.JwtTokenReader;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenReader jwtTokenReader;
  private final AuthenticationEntryPoint authenticationEntryPoint;

  public JwtAuthenticationFilter(
      JwtTokenReader jwtTokenReader, AuthenticationEntryPoint authenticationEntryPoint) {
    this.jwtTokenReader = jwtTokenReader;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var authorization = request.getHeader("Authorization");

    if (authorization == null || authorization.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    if (!authorization.startsWith("Bearer ")) {
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(
          request, response, new BadCredentialsException("Invalid authorization header."));
      return;
    }

    var token = authorization.substring(7);

    try {
      var principal = jwtTokenReader.read(token);
      var authenticatedUser =
          new AuthenticatedUser(
              principal.userId(), principal.email(), principal.tenantId(), principal.roles());

      var authentication =
          new JwtAuthenticationToken(authenticatedUser, authenticatedUser.getAuthorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (ExpiredJwtException ex) {
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(
          request, response, new BadCredentialsException("Expired token.", ex));
      return;
    } catch (JwtException | IllegalArgumentException ex) {
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(
          request, response, new BadCredentialsException("Invalid token.", ex));
      return;
    }

    filterChain.doFilter(request, response);
  }
}
