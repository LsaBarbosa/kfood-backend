package com.kfood.merchant.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.ChangeStoreStatusUseCase;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceCommand;
import com.kfood.merchant.app.CreateStoreTermsAcceptanceUseCase;
import com.kfood.merchant.app.CreateStoreUseCase;
import com.kfood.merchant.app.GetStoreDetailsUseCase;
import com.kfood.merchant.app.GetStoreTermsAcceptanceHistoryUseCase;
import com.kfood.merchant.app.StoreDetailsOutput;
import com.kfood.merchant.app.StoreTermsAcceptanceHistoryItemOutput;
import com.kfood.merchant.app.StoreTermsAcceptanceOutput;
import com.kfood.merchant.app.UpdateStoreUseCase;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.web.ForwardedClientIpResolver;
import java.time.Instant;
import java.util.List;
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
            provider(mock(GetStoreTermsAcceptanceHistoryUseCase.class)),
            provider(mock(ChangeStoreStatusUseCase.class)),
            new ForwardedClientIpResolver(false));
    var request = new CreateStoreTermsAcceptanceRequest(LegalDocumentType.TERMS_OF_USE, "2026.03");
    var httpServletRequest = new MockHttpServletRequest();
    httpServletRequest.addHeader("X-Forwarded-For", "198.51.100.10");
    httpServletRequest.addHeader("X-Real-IP", "198.51.100.11");
    httpServletRequest.setRemoteAddr("203.0.113.7");
    var expectedResponse =
        new StoreTermsAcceptanceOutput(
            UUID.randomUUID(),
            LegalDocumentType.TERMS_OF_USE,
            "2026.03",
            Instant.parse("2026-03-20T10:15:00Z"));

    when(createStoreTermsAcceptanceUseCase.execute(
            any(CreateStoreTermsAcceptanceCommand.class), same("203.0.113.7")))
        .thenReturn(expectedResponse);

    var response = controller.acceptTerms(request, httpServletRequest);

    assertThat(response.id()).isEqualTo(expectedResponse.id());
    assertThat(response.documentVersion()).isEqualTo(expectedResponse.documentVersion());
    verify(createStoreTermsAcceptanceUseCase)
        .execute(any(CreateStoreTermsAcceptanceCommand.class), same("203.0.113.7"));
  }

  @Test
  void shouldReturnTermsAcceptanceHistoryResponse() {
    GetStoreTermsAcceptanceHistoryUseCase getStoreTermsAcceptanceHistoryUseCase =
        mock(GetStoreTermsAcceptanceHistoryUseCase.class);
    var controller =
        new StoreController(
            provider(mock(CreateStoreUseCase.class)),
            provider(mock(UpdateStoreUseCase.class)),
            provider(mock(GetStoreDetailsUseCase.class)),
            provider(mock(CreateStoreTermsAcceptanceUseCase.class)),
            provider(getStoreTermsAcceptanceHistoryUseCase),
            provider(mock(ChangeStoreStatusUseCase.class)),
            new ForwardedClientIpResolver(false));
    var acceptanceId = UUID.randomUUID();
    var acceptedByUserId = UUID.randomUUID();
    when(getStoreTermsAcceptanceHistoryUseCase.execute())
        .thenReturn(
            List.of(
                new StoreTermsAcceptanceHistoryItemOutput(
                    acceptanceId,
                    acceptedByUserId,
                    LegalDocumentType.TERMS_OF_USE,
                    "2026.04",
                    Instant.parse("2026-04-20T13:15:00Z"))));

    var response = controller.getTermsAcceptanceHistory();

    assertThat(response)
        .singleElement()
        .satisfies(item -> assertThat(item.id()).isEqualTo(acceptanceId));
    assertThat(response.getFirst().acceptedByUserId()).isEqualTo(acceptedByUserId);
    assertThat(response.getFirst().documentVersion()).isEqualTo("2026.04");
  }

  @Test
  void shouldReturnNullAddressForLegacyStoreDetailsResponse() {
    GetStoreDetailsUseCase getStoreDetailsUseCase = mock(GetStoreDetailsUseCase.class);
    var controller =
        new StoreController(
            provider(mock(CreateStoreUseCase.class)),
            provider(mock(UpdateStoreUseCase.class)),
            provider(getStoreDetailsUseCase),
            provider(mock(CreateStoreTermsAcceptanceUseCase.class)),
            provider(mock(GetStoreTermsAcceptanceHistoryUseCase.class)),
            provider(mock(ChangeStoreStatusUseCase.class)),
            new ForwardedClientIpResolver(false));
    var storeId = UUID.randomUUID();
    when(getStoreDetailsUseCase.execute())
        .thenReturn(
            new StoreDetailsOutput(
                storeId,
                "loja-legada",
                "Loja Legada",
                StoreStatus.SETUP,
                "21999990000",
                "America/Sao_Paulo",
                false,
                false));

    var response = controller.getCurrentStore();

    assertThat(response.id()).isEqualTo(storeId);
    assertThat(response.address()).isNull();
  }

  @SuppressWarnings("unchecked")
  private <T> ObjectProvider<T> provider(T object) {
    ObjectProvider<T> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(object);
    return provider;
  }
}
