package com.kfood.merchant.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.application.user.CreateMerchantUserUseCase;
import com.kfood.merchant.application.user.ListMerchantUsersUseCase;
import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class MerchantUserControllerContextTest {

  @Autowired private MerchantUserController merchantUserController;
  @Autowired private CreateMerchantUserUseCase createMerchantUserUseCase;
  @Autowired private ListMerchantUsersUseCase listMerchantUsersUseCase;

  @MockitoBean private MerchantUserManagementPort merchantUserManagementPort;
  @MockitoBean private MerchantTenantAccessPort merchantTenantAccessPort;

  @Test
  void shouldLoadControllerAndTransactionalUseCasesFromSpringContext() {
    assertThat(merchantUserController).isNotNull();
    assertThat(createMerchantUserUseCase).isNotNull();
    assertThat(listMerchantUsersUseCase).isNotNull();
    assertThat(AopUtils.isAopProxy(createMerchantUserUseCase)).isTrue();
    assertThat(AopUtils.isAopProxy(listMerchantUsersUseCase)).isTrue();
  }
}
