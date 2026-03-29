package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantActivationRequirementsPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreActivationRequirementsServiceTest {

  private final MerchantActivationRequirementsPort merchantActivationRequirementsPort =
      mock(MerchantActivationRequirementsPort.class);
  private final StoreActivationRequirementsService service =
      new StoreActivationRequirementsService(merchantActivationRequirementsPort);

  @Test
  void shouldReturnRequirementsFromPort() {
    var storeId = UUID.randomUUID();
    var requirements = new StoreActivationRequirements(true, false, true);

    when(merchantActivationRequirementsPort.evaluate(storeId)).thenReturn(requirements);

    var response = service.evaluate(storeId);

    assertThat(response.hoursConfigured()).isTrue();
    assertThat(response.deliveryZonesConfigured()).isFalse();
    assertThat(response.termsAccepted()).isTrue();
    verify(merchantActivationRequirementsPort).evaluate(storeId);
  }
}
