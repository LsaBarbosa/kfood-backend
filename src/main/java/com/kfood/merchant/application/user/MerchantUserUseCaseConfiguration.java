package com.kfood.merchant.application.user;

import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MerchantUserUseCaseConfiguration {

  @Bean
  CreateMerchantUserUseCase createMerchantUserUseCase(
      MerchantUserManagementPort merchantUserManagementPort,
      MerchantTenantAccessPort merchantTenantAccessPort) {
    return new CreateMerchantUserUseCase(merchantUserManagementPort, merchantTenantAccessPort);
  }

  @Bean
  ListMerchantUsersUseCase listMerchantUsersUseCase(
      MerchantUserManagementPort merchantUserManagementPort,
      MerchantTenantAccessPort merchantTenantAccessPort) {
    return new ListMerchantUsersUseCase(merchantUserManagementPort, merchantTenantAccessPort);
  }
}
