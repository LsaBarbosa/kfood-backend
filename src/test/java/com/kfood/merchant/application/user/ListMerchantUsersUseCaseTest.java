package com.kfood.merchant.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserStatus;
import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListMerchantUsersUseCaseTest {

  private final MerchantUserManagementPort merchantUserManagementPort =
      mock(MerchantUserManagementPort.class);
  private final MerchantTenantAccessPort merchantTenantAccessPort =
      mock(MerchantTenantAccessPort.class);
  private final ListMerchantUsersUseCase useCase =
      new ListMerchantUsersUseCase(merchantUserManagementPort, merchantTenantAccessPort);

  @Test
  void shouldListUsersFromCorrectTenant() {
    var storeId = UUID.randomUUID();
    when(merchantTenantAccessPort.getRequiredStoreId()).thenReturn(storeId);
    when(merchantUserManagementPort.listByStoreId(storeId))
        .thenReturn(
            List.of(
                output("a@kfood.local", "ATTENDANT"), output("b@kfood.local", "MANAGER")));

    var response = useCase.execute();

    assertThat(response).hasSize(2);
    assertThat(response).extracting(item -> item.email()).containsExactly("a@kfood.local", "b@kfood.local");
    verify(merchantUserManagementPort).listByStoreId(storeId);
  }

  @Test
  void shouldReturnEmptyListWhenTenantHasNoUsers() {
    var storeId = UUID.randomUUID();
    when(merchantTenantAccessPort.getRequiredStoreId()).thenReturn(storeId);
    when(merchantUserManagementPort.listByStoreId(storeId)).thenReturn(List.of());

    var response = useCase.execute();

    assertThat(response).isEmpty();
  }

  @Test
  void shouldNotReturnUsersFromAnotherTenant() {
    var tenantStoreId = UUID.randomUUID();
    when(merchantTenantAccessPort.getRequiredStoreId()).thenReturn(tenantStoreId);
    when(merchantUserManagementPort.listByStoreId(tenantStoreId))
        .thenReturn(List.of(output("tenant@kfood.local", "MANAGER")));

    var response = useCase.execute();

    assertThat(response).hasSize(1);
    assertThat(response.getFirst().email()).isEqualTo("tenant@kfood.local");
    verify(merchantUserManagementPort).listByStoreId(tenantStoreId);
  }

  private MerchantUserOutput output(String email, String role) {
    return new MerchantUserOutput(
        UUID.randomUUID(), email, List.of(role), UserStatus.ACTIVE, Instant.parse("2026-03-26T12:00:00Z"));
  }
}
