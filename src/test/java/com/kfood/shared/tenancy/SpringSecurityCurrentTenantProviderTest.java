package com.kfood.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.kfood.identity.app.AuthenticatedUser;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SpringSecurityCurrentTenantProviderTest {

  private final IdentityUserRepository identityUserRepository =
      org.mockito.Mockito.mock(IdentityUserRepository.class);
  private final SpringSecurityCurrentTenantProvider provider =
      new SpringSecurityCurrentTenantProvider(providerOf(identityUserRepository));

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnStoreIdFromTenantAwarePrincipal() {
    var userId = UUID.randomUUID();
    var storeId = UUID.randomUUID();
    var principal = new AuthenticatedUser(userId, "owner@kfood.local", storeId, List.of("OWNER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    when(identityUserRepository.findById(userId))
        .thenReturn(Optional.of(userEntity(userId, storeId, UserRoleName.OWNER)));

    assertThat(provider.getRequiredStoreId()).isEqualTo(storeId);
  }

  @Test
  void shouldResolveStoreIdFromDatabaseWhenTokenHasNoTenantYet() {
    var userId = UUID.randomUUID();
    var storeId = UUID.randomUUID();
    var principal = new AuthenticatedUser(userId, "owner@kfood.local", null, List.of("OWNER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    when(identityUserRepository.findById(userId))
        .thenReturn(Optional.of(userEntity(userId, storeId, UserRoleName.OWNER)));

    assertThat(provider.getRequiredStoreId()).isEqualTo(storeId);
  }

  @Test
  void shouldReturnPrincipalStoreIdWhenRepositoryIsUnavailable() {
    var userId = UUID.randomUUID();
    var storeId = UUID.randomUUID();
    var providerWithoutRepository =
        new SpringSecurityCurrentTenantProvider(
            new ObjectProvider<>() {
              @Override
              public IdentityUserRepository getObject(Object... args) {
                return null;
              }

              @Override
              public IdentityUserRepository getIfAvailable() {
                return null;
              }

              @Override
              public IdentityUserRepository getIfUnique() {
                return null;
              }
            });
    var principal = new AuthenticatedUser(userId, "owner@kfood.local", storeId, List.of("OWNER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    assertThat(providerWithoutRepository.getRequiredStoreId()).isEqualTo(storeId);
  }

  @Test
  void shouldRejectWhenRepositoryIsUnavailableAndTokenHasNoTenant() {
    var userId = UUID.randomUUID();
    var providerWithoutRepository =
        new SpringSecurityCurrentTenantProvider(
            new ObjectProvider<>() {
              @Override
              public IdentityUserRepository getObject(Object... args) {
                return null;
              }

              @Override
              public IdentityUserRepository getIfAvailable() {
                return null;
              }

              @Override
              public IdentityUserRepository getIfUnique() {
                return null;
              }
            });
    var principal = new AuthenticatedUser(userId, "owner@kfood.local", null, List.of("OWNER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    assertThatThrownBy(providerWithoutRepository::getRequiredStoreId)
        .isInstanceOf(TenantScopeAccessDeniedException.class)
        .hasMessage("Authenticated user is not bound to a store");
  }

  @Test
  void shouldRejectWhenPrincipalTenantDiffersFromPersistedTenant() {
    var userId = UUID.randomUUID();
    var tokenStoreId = UUID.randomUUID();
    var persistedStoreId = UUID.randomUUID();
    var principal =
        new AuthenticatedUser(userId, "owner@kfood.local", tokenStoreId, List.of("OWNER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    when(identityUserRepository.findById(userId))
        .thenReturn(Optional.of(userEntity(userId, persistedStoreId, UserRoleName.OWNER)));

    assertThatThrownBy(provider::getRequiredStoreId)
        .isInstanceOf(TenantScopeAccessDeniedException.class)
        .hasMessage("Authenticated user cannot access another tenant");
  }

  @Test
  void shouldRejectWhenAuthenticatedUserCannotBeResolved() {
    var userId = UUID.randomUUID();
    var principal = new AuthenticatedUser(userId, "owner@kfood.local", null, List.of("OWNER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    when(identityUserRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(provider::getRequiredStoreId)
        .isInstanceOf(TenantScopeAccessDeniedException.class)
        .hasMessage("Authenticated user cannot be resolved");
  }

  @Test
  void shouldKeepPrincipalStoreIdWhenPersistedUserHasNoStore() {
    var userId = UUID.randomUUID();
    var storeId = UUID.randomUUID();
    var principal = new AuthenticatedUser(userId, "owner@kfood.local", storeId, List.of("OWNER"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    when(identityUserRepository.findById(userId))
        .thenReturn(Optional.of(userEntity(userId, null, UserRoleName.OWNER)));

    assertThat(provider.getRequiredStoreId()).isEqualTo(storeId);
  }

  @Test
  void shouldRejectWhenAuthenticationIsMissing() {
    assertThatThrownBy(provider::getRequiredStoreId)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Unauthenticated request");
  }

  @Test
  void shouldRejectWhenAuthenticationIsPresentButNotAuthenticated() {
    var authentication =
        new UsernamePasswordAuthenticationToken("plain-user", null, List.of()) {
          @Override
          public boolean isAuthenticated() {
            return false;
          }
        };
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThatThrownBy(provider::getRequiredStoreId)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Unauthenticated request");
  }

  @Test
  void shouldRejectWhenPrincipalIsNotTenantAware() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("plain-user", null, List.of()));

    assertThatThrownBy(provider::getRequiredStoreId)
        .isInstanceOf(TenantScopeAccessDeniedException.class)
        .hasMessage("Authenticated user is not bound to a store");
  }

  @Test
  void shouldRejectWhenTenantAwarePrincipalHasNoStore() {
    var principal =
        new AuthenticatedUser(UUID.randomUUID(), "admin@kfood.local", null, List.of("ADMIN"));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    when(identityUserRepository.findById(principal.getUserId()))
        .thenReturn(Optional.of(userEntity(principal.getUserId(), null, UserRoleName.ADMIN)));

    assertThatThrownBy(provider::getRequiredStoreId)
        .isInstanceOf(TenantScopeAccessDeniedException.class)
        .hasMessage("Authenticated user is not bound to a store");
  }

  private IdentityUserEntity userEntity(UUID userId, UUID storeId, UserRoleName roleName) {
    var user =
        new IdentityUserEntity(
            userId, storeId, "owner@kfood.local", "$2a$10$hash", UserStatus.ACTIVE);
    user.replaceRoles(Set.of(roleName));
    return user;
  }

  private ObjectProvider<IdentityUserRepository> providerOf(IdentityUserRepository repository) {
    return new ObjectProvider<>() {
      @Override
      public IdentityUserRepository getObject(Object... args) {
        return repository;
      }

      @Override
      public IdentityUserRepository getIfAvailable() {
        return repository;
      }

      @Override
      public IdentityUserRepository getIfUnique() {
        return repository;
      }
    };
  }
}
