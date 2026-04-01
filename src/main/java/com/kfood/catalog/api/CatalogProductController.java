package com.kfood.catalog.api;

import com.kfood.catalog.app.CreateCatalogProductUseCase;
import com.kfood.catalog.app.DeactivateCatalogProductUseCase;
import com.kfood.catalog.app.ListCatalogProductsUseCase;
import com.kfood.catalog.app.UpdateCatalogProductAvailabilityUseCase;
import com.kfood.catalog.app.UpdateCatalogProductPauseUseCase;
import com.kfood.catalog.app.UpdateCatalogProductUseCase;
import com.kfood.identity.app.Roles;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/catalog/products")
public class CatalogProductController {

  private final ObjectProvider<CreateCatalogProductUseCase> createCatalogProductUseCaseProvider;
  private final ObjectProvider<ListCatalogProductsUseCase> listCatalogProductsUseCaseProvider;
  private final ObjectProvider<UpdateCatalogProductUseCase> updateCatalogProductUseCaseProvider;
  private final ObjectProvider<UpdateCatalogProductPauseUseCase>
      updateCatalogProductPauseUseCaseProvider;
  private final ObjectProvider<UpdateCatalogProductAvailabilityUseCase>
      updateCatalogProductAvailabilityUseCaseProvider;
  private final ObjectProvider<DeactivateCatalogProductUseCase>
      deactivateCatalogProductUseCaseProvider;

  public CatalogProductController(
      ObjectProvider<CreateCatalogProductUseCase> createCatalogProductUseCaseProvider,
      ObjectProvider<ListCatalogProductsUseCase> listCatalogProductsUseCaseProvider,
      ObjectProvider<UpdateCatalogProductUseCase> updateCatalogProductUseCaseProvider,
      ObjectProvider<UpdateCatalogProductPauseUseCase> updateCatalogProductPauseUseCaseProvider,
      ObjectProvider<UpdateCatalogProductAvailabilityUseCase>
          updateCatalogProductAvailabilityUseCaseProvider,
      ObjectProvider<DeactivateCatalogProductUseCase> deactivateCatalogProductUseCaseProvider) {
    this.createCatalogProductUseCaseProvider = createCatalogProductUseCaseProvider;
    this.listCatalogProductsUseCaseProvider = listCatalogProductsUseCaseProvider;
    this.updateCatalogProductUseCaseProvider = updateCatalogProductUseCaseProvider;
    this.updateCatalogProductPauseUseCaseProvider = updateCatalogProductPauseUseCaseProvider;
    this.updateCatalogProductAvailabilityUseCaseProvider =
        updateCatalogProductAvailabilityUseCaseProvider;
    this.deactivateCatalogProductUseCaseProvider = deactivateCatalogProductUseCaseProvider;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CatalogProductResponse create(@Valid @RequestBody CreateCatalogProductRequest request) {
    return createCatalogProductUseCase().execute(request);
  }

  @GetMapping
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public List<CatalogProductResponse> list() {
    return listCatalogProductsUseCase().execute();
  }

  @PutMapping("/{productId}")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CatalogProductResponse update(
      @PathVariable UUID productId, @Valid @RequestBody UpdateCatalogProductRequest request) {
    return updateCatalogProductUseCase().execute(productId, request);
  }

  @PatchMapping("/{productId}/inactive")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CatalogProductResponse deactivate(@PathVariable UUID productId) {
    return deactivateCatalogProductUseCase().execute(productId);
  }

  @PatchMapping("/{productId}/pause")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CatalogProductPauseResponse updatePause(
      @PathVariable UUID productId, @Valid @RequestBody UpdateCatalogProductPauseRequest request) {
    return updateCatalogProductPauseUseCase().execute(productId, request);
  }

  @PutMapping("/{productId}/availability-windows")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CatalogProductAvailabilityResponse updateAvailabilityWindows(
      @PathVariable UUID productId,
      @Valid @RequestBody UpdateCatalogProductAvailabilityRequest request) {
    return updateCatalogProductAvailabilityUseCase().execute(productId, request);
  }

  private CreateCatalogProductUseCase createCatalogProductUseCase() {
    return createCatalogProductUseCaseProvider.getObject();
  }

  private ListCatalogProductsUseCase listCatalogProductsUseCase() {
    return listCatalogProductsUseCaseProvider.getObject();
  }

  private UpdateCatalogProductUseCase updateCatalogProductUseCase() {
    return updateCatalogProductUseCaseProvider.getObject();
  }

  private DeactivateCatalogProductUseCase deactivateCatalogProductUseCase() {
    return deactivateCatalogProductUseCaseProvider.getObject();
  }

  private UpdateCatalogProductPauseUseCase updateCatalogProductPauseUseCase() {
    return updateCatalogProductPauseUseCaseProvider.getObject();
  }

  private UpdateCatalogProductAvailabilityUseCase updateCatalogProductAvailabilityUseCase() {
    return updateCatalogProductAvailabilityUseCaseProvider.getObject();
  }
}
