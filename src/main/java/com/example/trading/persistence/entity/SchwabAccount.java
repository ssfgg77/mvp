package com.example.trading.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "schwab_account",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_schwab_account_user_ref", columnNames = {"user_id", "schwab_account_ref"})
    })
public class SchwabAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_schwab_account_user"))
  private AppUser user;

  @Column(name = "schwab_account_ref", nullable = false, length = 128)
  private String schwabAccountRef; // account hash/id reference from Schwab

  @Column(name = "account_type", length = 64)
  private String accountType;

  @Column(name = "nickname", length = 128)
  private String nickname;

  // Balance snapshot fields (updated by 30s refresh job)
  @Column(name = "cash_balance", precision = 19, scale = 4)
  private BigDecimal cashBalance;

  @Column(name = "equity_value", precision = 19, scale = 4)
  private BigDecimal equityValue;

  @Column(name = "last_refreshed_at")
  private Instant lastRefreshedAt;

  public Long getId() { return id; }
  public AppUser getUser() { return user; }
  public void setUser(AppUser user) { this.user = user; }
  public String getSchwabAccountRef() { return schwabAccountRef; }
  public void setSchwabAccountRef(String schwabAccountRef) { this.schwabAccountRef = schwabAccountRef; }
  public String getAccountType() { return accountType; }
  public void setAccountType(String accountType) { this.accountType = accountType; }
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  public BigDecimal getCashBalance() { return cashBalance; }
  public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
  public BigDecimal getEquityValue() { return equityValue; }
  public void setEquityValue(BigDecimal equityValue) { this.equityValue = equityValue; }
  public Instant getLastRefreshedAt() { return lastRefreshedAt; }
  public void setLastRefreshedAt(Instant lastRefreshedAt) { this.lastRefreshedAt = lastRefreshedAt; }
}
