package com.kfood.catalog.api;

import com.kfood.catalog.app.CreateCatalogCategoryUseCase;
import com.kfood.catalog.app.DeactivateCatalogCategoryUseCase;
import com.kfood.catalog.app.ListCatalogCategoriesUseCase;
import com.kfood.catalog.app.UpdateCatalogCategoryUseCase;
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
@RequestMapping("/v1/catalog/categories")
public class CatalogCategoryController {

  private final ObjectProvider<CreateCatalogCategoryUseCase> createCatalogCategoryUseCaseProvider;
  private final ObjectProvider<ListCatalogCategoriesUseCase> listCatalogCategoriesUseCaseProvider;
  private final ObjectProvider<UpdateCatalogCategoryUseCase> updateCatalogCategoryUseCaseProvider;
  private final ObjectProvider<DeactivateCatalogCategoryUseCase>
      deactivateCatalogCategoryUseCaseProvider;

  public CatalogCategoryController(
      ObjectProvider<CreateCatalogCategoryUseCase> createCatalogCategoryUseCaseProvider,
      ObjectProvider<ListCatalogCategoriesUseCase> listCatalogCategoriesUseCaseProvider,
      ObjectProvider<UpdateCatalogCategoryUseCase> updateCatalogCategoryUseCaseProvider,
      ObjectProvider<DeactivateCatalogCategoryUseCase> deactivateCatalogCategoryUseCaseProvider) {
    this.createCatalogCategoryUseCaseProvider = createCatalogCategoryUseCaseProvider;
    this.listCatalogCategoriesUseCaseProvider = listCatalogCategoriesUseCaseProvider;
    this.updateCatalogCategoryUseCaseProvider = updateCatalogCategoryUseCaseProvider;
    this.deactivateCatalogCategoryUseCaseProvider = deactivateCatalogCategoryUseCaseProvider;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CatalogCategoryResponse create(@Valid @RequestBody CreateCatalogCategoryRequest request) {
    return createCatalogCategoryUseCase().execute(request);
  }

  @GetMapping
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public List<CatalogCategoryResponse> list() {
    return listCatalogCategoriesUseCase().execute();
  }

  @PutMapping("/{categoryId}")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CatalogCategoryResponse update(
      @PathVariable UUID categoryId, @Valid @RequestBody UpdateCatalogCategoryRequest request) {
    return updateCatalogCategoryUseCase().execute(categoryId, request);
  }

  @PatchMapping("/{categoryId}/inactive")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public CatalogCategoryResponse deactivate(@PathVariable UUID categoryId) {
    return deactivateCatalogCategoryUseCase().execute(categoryId);
  }

  private CreateCatalogCategoryUseCase createCatalogCategoryUseCase() {
    return createCatalogCategoryUseCaseProvider.getObject();
  }

  private ListCatalogCategoriesUseCase listCatalogCategoriesUseCase() {
    return listCatalogCategoriesUseCaseProvider.getObject();
  }

  private UpdateCatalogCategoryUseCase updateCatalogCategoryUseCase() {
    return updateCatalogCategoryUseCaseProvider.getObject();
  }

  private DeactivateCatalogCategoryUseCase deactivateCatalogCategoryUseCase() {
    return deactivateCatalogCategoryUseCaseProvider.getObject();
  }
}
