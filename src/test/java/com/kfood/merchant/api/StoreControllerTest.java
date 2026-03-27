package com.kfood.merchant.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.ChangeStoreStatusUseCase;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceUseCase;
import com.kfood.merchant.app.CreateStoreUseCase;
import com.kfood.merchant.app.GetStoreDetailsUseCase;
import com.kfood.merchant.app.UpdateStoreUseCase;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.shared.web.ForwardedClientIpResolver;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

class StoreControllerTest {

  @Test
  void shouldPassResolvedRemoteAddrToTermsAcceptanceUseCaseWhenForwardedHeadersAreUntrusted() {
    CreateStoreTermsAcceptanceUseCase createStoreTermsAcceptanceUseCase =
        mock(CreateStoreTermsAcceptanceUseCase.class);
    var controller =
        new StoreController(
            provider(mock(CreateStoreUseCase.class)),
            provider(mock(UpdateStoreUseCase.class)),
            provider(mock(GetStoreDetailsUseCase.class)),
            provider(createStoreTermsAcceptanceUseCase),
            provider(mock(ChangeStoreStatusUseCase.class)),
            new ForwardedClientIpResolver(false));
    var request = new CreateStoreTermsAcceptanceRequest(LegalDocumentType.TERMS_OF_USE, "2026.03");
    var httpServletRequest = new MockHttpServletRequest();
    httpServletRequest.addHeader("X-Forwarded-For", "198.51.100.10");
    httpServletRequest.addHeader("X-Real-IP", "198.51.100.11");
    httpServletRequest.setRemoteAddr("203.0.113.7");
    var expectedResponse =
        new StoreTermsAcceptanceResponse(
            UUID.randomUUID(),
            LegalDocumentType.TERMS_OF_USE,
            "2026.03",
            Instant.parse("2026-03-20T10:15:00Z"));

    when(createStoreTermsAcceptanceUseCase.execute(same(request), same("203.0.113.7")))
        .thenReturn(expectedResponse);

    var response = controller.acceptTerms(request, httpServletRequest);

    assertThat(response).isEqualTo(expectedResponse);
    verify(createStoreTermsAcceptanceUseCase).execute(request, "203.0.113.7");
  }

  @SuppressWarnings("unchecked")
  private <T> ObjectProvider<T> provider(T object) {
    ObjectProvider<T> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(object);
    return provider;
  }
}
