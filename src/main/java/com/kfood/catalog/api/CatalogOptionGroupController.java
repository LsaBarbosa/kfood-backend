package com.kfood.catalog.api;

import com.kfood.catalog.app.CreateCatalogOptionGroupUseCase;
import com.kfood.identity.app.Roles;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/catalog/products/{productId}/option-groups")
public class CatalogOptionGroupController {

  private final ObjectProvider<CreateCatalogOptionGroupUseCase>
      createCatalogOptionGroupUseCaseProvider;

  public CatalogOptionGroupController(
      ObjectProvider<CreateCatalogOptionGroupUseCase> createCatalogOptionGroupUseCaseProvider) {
    this.createCatalogOptionGroupUseCaseProvider = createCatalogOptionGroupUseCaseProvider;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CatalogOptionGroupResponse create(
      @PathVariable UUID productId, @Valid @RequestBody CreateCatalogOptionGroupRequest request) {
    return createCatalogOptionGroupUseCase().execute(productId, request);
  }

  private CreateCatalogOptionGroupUseCase createCatalogOptionGroupUseCase() {
    return createCatalogOptionGroupUseCaseProvider.getObject();
  }
}
