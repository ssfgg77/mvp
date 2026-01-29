package com.example.trading.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trade_order",
    indexes = {
        @Index(name = "ix_trade_order_user_status_created", columnList = "user_id,status,created_at")
    })
public class TradeOrder {

  public enum Status {
    NEW,
    SUBMITTED,
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCEL_REQUESTED,
    CANCELED,
    REJECTED,
    ERROR
  }

  public enum Side { BUY, SELL }
  public enum OrderType { MARKET, LIMIT }

  @Id
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_trade_order_user"))
  private AppUser user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "schwab_account_id", foreignKey = @ForeignKey(name = "fk_trade_order_account"))
  private SchwabAccount schwabAccount;

  @Column(name = "client_order_id", nullable = false, length = 64)
  private String clientOrderId;

  @Column(name = "schwab_order_id", length = 128)
  private String schwabOrderId;

  @Column(nullable = false, length = 16)
  @Enumerated(EnumType.STRING)
  private Side side;

  @Column(nullable = false, length = 16)
  @Enumerated(EnumType.STRING)
  private OrderType orderType;

  @Column(nullable = false, length = 32)
  private String symbol;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "limit_price", precision = 19, scale = 4)
  private BigDecimal limitPrice;

  @Column(nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private Status status = Status.NEW;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public TradeOrder() {
    this.id = UUID.randomUUID();
  }

  public UUID getId() { return id; }
  public AppUser getUser() { return user; }
  public void setUser(AppUser user) { this.user = user; }
  public SchwabAccount getSchwabAccount() { return schwabAccount; }
  public void setSchwabAccount(SchwabAccount schwabAccount) { this.schwabAccount = schwabAccount; }
  public String getClientOrderId() { return clientOrderId; }
  public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
  public String getSchwabOrderId() { return schwabOrderId; }
  public void setSchwabOrderId(String schwabOrderId) { this.schwabOrderId = schwabOrderId; }
  public Side getSide() { return side; }
  public void setSide(Side side) { this.side = side; }
  public OrderType getOrderType() { return orderType; }
  public void setOrderType(OrderType orderType) { this.orderType = orderType; }
  public String getSymbol() { return symbol; }
  public void setSymbol(String symbol) { this.symbol = symbol; }
  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public BigDecimal getLimitPrice() { return limitPrice; }
  public void setLimitPrice(BigDecimal limitPrice) { this.limitPrice = limitPrice; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
