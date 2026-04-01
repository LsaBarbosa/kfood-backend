package com.kfood.identity.infra.security;

import com.kfood.identity.app.AuthenticatedUser;
import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {

  public JwtAuthenticationToken(
      AuthenticatedUser principal, Collection<? extends GrantedAuthority> authorities) {
    super(principal, null, authorities);
  }

  @Override
  public AuthenticatedUser getPrincipal() {
    return (AuthenticatedUser) super.getPrincipal();
  }
}
