package com.kfood.checkout.app;

import com.kfood.catalog.app.availability.CatalogProductAvailabilityValidator;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.checkout.api.QuoteCheckoutResponse;
import com.kfood.customer.infra.persistence.CustomerRepository;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CustomerRepository.class,
  CatalogProductRepository.class,
  StoreBusinessHourRepository.class
})
public class CalculateCheckoutQuoteUseCase {

  private final StoreRepository storeRepository;
  private final CustomerRepository customerRepository;
  private final CatalogProductRepository catalogProductRepository;
  private final CatalogProductAvailabilityValidator catalogProductAvailabilityValidator;
  private final StoreCheckoutRulesValidator storeCheckoutRulesValidator;
  private final QuoteFulfillmentPolicy quoteFulfillmentPolicy;
  private final CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway;

  public CalculateCheckoutQuoteUseCase(
      StoreRepository storeRepository,
      CustomerRepository customerRepository,
      CatalogProductRepository catalogProductRepository,
      CatalogProductAvailabilityValidator catalogProductAvailabilityValidator,
      StoreCheckoutRulesValidator storeCheckoutRulesValidator,
      QuoteFulfillmentPolicy quoteFulfillmentPolicy,
      CheckoutQuoteSnapshotGateway checkoutQuoteSnapshotGateway) {
    this.storeRepository = storeRepository;
    this.customerRepository = customerRepository;
    this.catalogProductRepository = catalogProductRepository;
    this.catalogProductAvailabilityValidator = catalogProductAvailabilityValidator;
    this.storeCheckoutRulesValidator = storeCheckoutRulesValidator;
    this.quoteFulfillmentPolicy = quoteFulfillmentPolicy;
    this.checkoutQuoteSnapshotGateway = checkoutQuoteSnapshotGateway;
  }

  @Transactional(readOnly = true)
  public QuoteCheckoutResponse execute(String storeSlug, CalculateCheckoutQuoteCommand command) {
    var normalizedSlug = normalize(storeSlug, "storeSlug is required");
    var store =
        storeRepository
            .findBySlug(normalizedSlug)
            .orElseThrow(() -> new StoreSlugNotFoundException(normalizedSlug));
    storeCheckoutRulesValidator.ensureStoreOperational(store);
    storeCheckoutRulesValidator.ensureStoreWithinBusinessHours(store);
    var customer =
        customerRepository
            .findByIdAndStoreId(command.customerId(), store.getId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Customer not found for this store.",
                        HttpStatus.NOT_FOUND));

    var productIds =
        command.items().stream()
            .map(CalculateCheckoutQuoteItemCommand::productId)
            .distinct()
            .toList();
    var products =
        catalogProductRepository.findAllByStoreIdAndIdIn(store.getId(), productIds).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    CatalogProduct::getId,
                    java.util.function.Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new));
    if (products.size() != productIds.size()) {
      throw new BusinessException(
          ErrorCode.RESOURCE_NOT_FOUND, "Product not found for this store.", HttpStatus.NOT_FOUND);
    }

    var quoteId = UUID.randomUUID();
    var subtotalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    var totalUnits = 0;
    var itemSnapshots = new java.util.ArrayList<CheckoutQuoteItemSnapshot>();
    for (var item : command.items()) {
      var product = products.get(item.productId());
      catalogProductAvailabilityValidator.ensureAvailableNow(product, store.getTimezone());
      var itemSnapshot = toItemSnapshot(product, item);
      var itemTotal = calculateItemTotal(itemSnapshot);
      subtotalAmount = subtotalAmount.add(itemTotal).setScale(2, RoundingMode.HALF_UP);
      totalUnits += item.quantity();
      itemSnapshots.add(itemSnapshot);
    }

    var fulfillment =
        quoteFulfillmentPolicy.resolve(
            store,
            customer,
            command.fulfillmentType(),
            command.addressId(),
            subtotalAmount,
            totalUnits);
    var totalAmount =
        subtotalAmount.add(fulfillment.deliveryFee()).setScale(2, RoundingMode.HALF_UP);
    checkoutQuoteSnapshotGateway.save(
        new CheckoutQuoteSnapshot(
            quoteId,
            store.getId(),
            customer.getId(),
            command.fulfillmentType(),
            command.addressId(),
            subtotalAmount,
            fulfillment.deliveryFee(),
            totalAmount,
            List.copyOf(itemSnapshots),
            OffsetDateTime.now().plusMinutes(15)));

    return new QuoteCheckoutResponse(
        quoteId,
        store.getId(),
        subtotalAmount,
        fulfillment.deliveryFee(),
        totalAmount,
        fulfillment.estimatedPreparationMinutes(),
        fulfillment.messages());
  }

  private CheckoutQuoteItemSnapshot toItemSnapshot(
      CatalogProduct product, CalculateCheckoutQuoteItemCommand itemCommand) {
    ensureProductSellable(product);
    var optionCountByGroup = new HashMap<UUID, Integer>();
    var availableItemsById = indexAvailableItemsById(product);
    var optionSnapshots = new java.util.ArrayList<CheckoutQuoteOptionSnapshot>();

    for (var optionCommand : itemCommand.options()) {
      var optionItem = availableItemsById.get(optionCommand.optionItemId());
      if (optionItem == null) {
        throw new BusinessException(
            ErrorCode.CATALOG_ITEM_UNAVAILABLE,
            "Option item is not available for this product.",
            HttpStatus.UNPROCESSABLE_CONTENT);
      }

      optionCountByGroup.merge(
          optionItem.getOptionGroup().getId(), optionCommand.quantity(), Integer::sum);
      optionSnapshots.add(
          new CheckoutQuoteOptionSnapshot(
              optionItem.getName(), optionItem.getExtraPrice(), optionCommand.quantity()));
    }

    validateSelectionCounts(product, optionCountByGroup);
    return new CheckoutQuoteItemSnapshot(
        product.getId(),
        product.getName(),
        product.getBasePrice(),
        itemCommand.quantity(),
        itemCommand.notes(),
        List.copyOf(optionSnapshots));
  }

  private BigDecimal calculateItemTotal(CheckoutQuoteItemSnapshot itemSnapshot) {
    var baseTotal =
        itemSnapshot
            .unitPriceSnapshot()
            .multiply(BigDecimal.valueOf(itemSnapshot.quantity()))
            .setScale(2, RoundingMode.HALF_UP);
    var optionTotals = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    for (var optionSnapshot : itemSnapshot.options()) {
      var optionLineTotal =
          optionSnapshot
              .extraPriceSnapshot()
              .multiply(BigDecimal.valueOf(optionSnapshot.quantity()))
              .multiply(BigDecimal.valueOf(itemSnapshot.quantity()))
              .setScale(2, RoundingMode.HALF_UP);
      optionTotals = optionTotals.add(optionLineTotal).setScale(2, RoundingMode.HALF_UP);
    }
    return baseTotal.add(optionTotals).setScale(2, RoundingMode.HALF_UP);
  }

  private void ensureProductSellable(CatalogProduct product) {
    if (!product.isActive() || product.isPaused()) {
      throw new BusinessException(
          ErrorCode.CATALOG_ITEM_UNAVAILABLE,
          "Product is not available for checkout.",
          HttpStatus.UNPROCESSABLE_CONTENT);
    }
  }

  private Map<UUID, CatalogOptionItem> indexAvailableItemsById(CatalogProduct product) {
    var availableItems = new LinkedHashMap<UUID, CatalogOptionItem>();
    for (var group : product.getOptionGroups()) {
      if (!group.isActive()) {
        continue;
      }
      for (var item : group.getItems()) {
        if (item.isActive()) {
          availableItems.put(item.getId(), item);
        }
      }
    }
    return availableItems;
  }

  private void validateSelectionCounts(
      CatalogProduct product, Map<UUID, Integer> optionCountByGroup) {
    for (var group : product.getOptionGroups()) {
      if (!group.isActive()) {
        continue;
      }
      var selectedCount = optionCountByGroup.getOrDefault(group.getId(), 0);
      if (selectedCount < group.getMinSelect()) {
        throw new BusinessException(
            ErrorCode.VALIDATION_ERROR,
            "Selected options are below the minimum required for group: " + group.getName(),
            HttpStatus.BAD_REQUEST);
      }
      if (selectedCount > group.getMaxSelect()) {
        throw new BusinessException(
            ErrorCode.VALIDATION_ERROR,
            "Selected options exceed the maximum allowed for group: " + group.getName(),
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  private String normalize(String value, String message) {
    return Objects.requireNonNull(value, message).trim();
  }
}
