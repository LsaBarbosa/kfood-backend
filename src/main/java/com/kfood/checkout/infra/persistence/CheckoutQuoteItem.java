package com.kfood.checkout.infra.persistence;

import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "checkout_quote_item")
public class CheckoutQuoteItem extends AuditableEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quote_id", nullable = false)
  private CheckoutQuote quote;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "product_name_snapshot", nullable = false, length = 255)
  private String productNameSnapshot;

  @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPriceSnapshot;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "notes", length = 1000)
  private String notes;

  @OneToMany(
      mappedBy = "quoteItem",
      cascade = jakarta.persistence.CascadeType.ALL,
      orphanRemoval = true)
  private final List<CheckoutQuoteItemOption> options = new ArrayList<>();

  protected CheckoutQuoteItem() {}

  public CheckoutQuoteItem(
      UUID id,
      UUID productId,
      String productNameSnapshot,
      BigDecimal unitPriceSnapshot,
      Integer quantity,
      String notes) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.productId = Objects.requireNonNull(productId, "productId is required");
    this.productNameSnapshot =
        Objects.requireNonNull(productNameSnapshot, "productName is required").trim();
    this.unitPriceSnapshot =
        Objects.requireNonNull(unitPriceSnapshot, "unitPriceSnapshot is required")
            .setScale(2, RoundingMode.HALF_UP);
    this.quantity = Objects.requireNonNull(quantity, "quantity is required");
    this.notes = notes == null || notes.isBlank() ? null : notes.trim();
  }

  void attachToQuote(CheckoutQuote quote) {
    this.quote = Objects.requireNonNull(quote, "quote is required");
  }

  public void addOption(CheckoutQuoteItemOption option) {
    var validatedOption = Objects.requireNonNull(option, "option is required");
    validatedOption.attachToQuoteItem(this);
    options.add(validatedOption);
  }

  public UUID getId() {
    return id;
  }

  public UUID getProductId() {
    return productId;
  }

  public String getProductNameSnapshot() {
    return productNameSnapshot;
  }

  public BigDecimal getUnitPriceSnapshot() {
    return unitPriceSnapshot;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public String getNotes() {
    return notes;
  }

  public List<CheckoutQuoteItemOption> getOptions() {
    return Collections.unmodifiableList(options);
  }
}
