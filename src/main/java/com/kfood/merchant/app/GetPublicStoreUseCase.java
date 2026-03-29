package com.kfood.merchant.app;

import com.kfood.merchant.app.port.MerchantQueryPort;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(MerchantQueryPort.class)
public class GetPublicStoreUseCase {

  private final MerchantQueryPort merchantQueryPort;

  public GetPublicStoreUseCase(MerchantQueryPort merchantQueryPort) {
    this.merchantQueryPort = merchantQueryPort;
  }

  @Transactional(readOnly = true)
  public PublicStoreOutput execute(String slug) {
    var normalizedSlug = normalize(slug);
    return merchantQueryPort.getPublicStore(normalizedSlug);
  }

  private String normalize(String slug) {
    return Objects.requireNonNull(slug, "slug is required").trim();
  }
}
