package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantQueryPort;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(MerchantQueryPort.class)
public class GetPublicStoreMenuUseCase {

  private final MerchantQueryPort merchantQueryPort;

  public GetPublicStoreMenuUseCase(MerchantQueryPort merchantQueryPort) {
    this.merchantQueryPort = merchantQueryPort;
  }

  @Transactional(readOnly = true)
  public PublicStoreMenuOutput execute(String slug) {
    var normalizedSlug = normalize(slug);
    return merchantQueryPort.getPublicStoreMenu(normalizedSlug);
  }

  private String normalize(String slug) {
    return Objects.requireNonNull(slug, "slug is required").trim();
  }
}
