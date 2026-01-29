package com.example.trading.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "execution_fill",
    indexes = {
        @Index(name = "ix_execution_fill_order_time", columnList = "order_id,exec_time")
    })
public class ExecutionFill {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "fk_execution_fill_order"))
  private TradeOrder order;

  @Column(name = "external_exec_id", length = 128)
  private String externalExecId;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal price;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "exec_time", nullable = false)
  private Instant execTime;

  public Long getId() { return id; }
  public TradeOrder getOrder() { return order; }
  public void setOrder(TradeOrder order) { this.order = order; }
  public String getExternalExecId() { return externalExecId; }
  public void setExternalExecId(String externalExecId) { this.externalExecId = externalExecId; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public Instant getExecTime() { return execTime; }
  public void setExecTime(Instant execTime) { this.execTime = execTime; }
}
