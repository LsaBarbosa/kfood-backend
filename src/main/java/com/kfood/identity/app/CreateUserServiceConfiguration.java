package com.kfood.identity.app;

import com.kfood.identity.persistence.IdentityUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CreateUserServiceConfiguration {

  @Bean
  CreateUserService createUserService(
      IdentityUserRepository identityUserRepository, PasswordHashService passwordHashService) {
    return new CreateUserService(identityUserRepository, passwordHashService);
  }
}
