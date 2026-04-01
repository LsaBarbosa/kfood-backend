package com.kfood.checkout.app;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddressRepository;
import com.kfood.merchant.infra.persistence.DeliveryZoneRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.shared.exceptions.ApiFieldError;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class QuoteFulfillmentPolicy {

  private final CustomerAddressRepository customerAddressRepository;
  private final DeliveryZoneRepository deliveryZoneRepository;

  public QuoteFulfillmentPolicy(
      CustomerAddressRepository customerAddressRepository,
      DeliveryZoneRepository deliveryZoneRepository) {
    this.customerAddressRepository = customerAddressRepository;
    this.deliveryZoneRepository = deliveryZoneRepository;
  }

  public QuoteFulfillmentResult resolve(
      Store store,
      Customer customer,
      FulfillmentType fulfillmentType,
      UUID addressId,
      BigDecimal subtotalAmount,
      int totalUnits) {
    if (fulfillmentType == FulfillmentType.PICKUP) {
      return new QuoteFulfillmentResult(
          BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
          null,
          estimatePreparationMinutes(FulfillmentType.PICKUP, totalUnits),
          List.of("Pickup at the store."));
    }

    if (addressId == null) {
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "addressId is required when fulfillmentType is DELIVERY.",
          HttpStatus.BAD_REQUEST,
          List.of(new ApiFieldError("addressId", "Address is required for delivery.")));
    }

    var address =
        customerAddressRepository
            .findByIdAndCustomerId(addressId, customer.getId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Address not found for this customer.",
                        HttpStatus.NOT_FOUND));

    return deliveryZoneRepository
        .findByStoreIdAndZoneNameIgnoreCaseAndActiveTrue(store.getId(), address.getDistrict())
        .map(
            zone -> {
              var minimumOrderAmount = zone.getMinOrderAmount().setScale(2, RoundingMode.HALF_UP);
              if (subtotalAmount.compareTo(minimumOrderAmount) < 0) {
                throw new BusinessException(
                    ErrorCode.MIN_ORDER_NOT_REACHED,
                    "Order total is below the minimum for the selected delivery zone.",
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    List.of(
                        new ApiFieldError(
                            "items",
                            "Total current: "
                                + subtotalAmount.setScale(2, RoundingMode.HALF_UP)
                                + ", minimum: "
                                + minimumOrderAmount)));
              }
              return new QuoteFulfillmentResult(
                  zone.getFeeAmount().setScale(2, RoundingMode.HALF_UP),
                  address.getId(),
                  estimatePreparationMinutes(FulfillmentType.DELIVERY, totalUnits),
                  List.of("Delivery to zone " + zone.getZoneName() + "."));
            })
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.DELIVERY_ZONE_NOT_SUPPORTED,
                    "Address is outside the supported delivery area.",
                    HttpStatus.UNPROCESSABLE_CONTENT));
  }

  private int estimatePreparationMinutes(FulfillmentType fulfillmentType, int totalUnits) {
    var base = fulfillmentType == FulfillmentType.PICKUP ? 20 : 35;
    var extra = Math.max(0, totalUnits - 1) * 2;
    return base + extra;
  }

  public record QuoteFulfillmentResult(
      BigDecimal deliveryFee,
      UUID validatedAddressId,
      int estimatedPreparationMinutes,
      List<String> messages) {}
}
