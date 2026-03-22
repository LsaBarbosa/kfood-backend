package com.kfood.checkout.infra.persistence;

import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "checkout_quote_item_option")
public class CheckoutQuoteItemOption extends AuditableEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quote_item_id", nullable = false)
  private CheckoutQuoteItem quoteItem;

  @Column(name = "option_name_snapshot", nullable = false, length = 255)
  private String optionNameSnapshot;

  @Column(name = "extra_price_snapshot", nullable = false, precision = 12, scale = 2)
  private BigDecimal extraPriceSnapshot;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  protected CheckoutQuoteItemOption() {}

  public CheckoutQuoteItemOption(
      UUID id, String optionNameSnapshot, BigDecimal extraPriceSnapshot, Integer quantity) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.optionNameSnapshot =
        Objects.requireNonNull(optionNameSnapshot, "optionNameSnapshot is required").trim();
    this.extraPriceSnapshot =
        Objects.requireNonNull(extraPriceSnapshot, "extraPriceSnapshot is required")
            .setScale(2, RoundingMode.HALF_UP);
    this.quantity = Objects.requireNonNull(quantity, "quantity is required");
  }

  void attachToQuoteItem(CheckoutQuoteItem quoteItem) {
    this.quoteItem = Objects.requireNonNull(quoteItem, "quoteItem is required");
  }

  public String getOptionNameSnapshot() {
    return optionNameSnapshot;
  }

  public BigDecimal getExtraPriceSnapshot() {
    return extraPriceSnapshot;
  }

  public Integer getQuantity() {
    return quantity;
  }
}
