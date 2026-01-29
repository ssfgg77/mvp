package com.example.trading.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_settings")
public class UserSettings {
  @Id
  private Long userId;

  @OneToOne(optional = false)
  @MapsId
  @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_settings_user"))
  private AppUser user;

  @ManyToOne
  @JoinColumn(name = "default_schwab_account_id", foreignKey = @ForeignKey(name = "fk_user_settings_default_account"))
  private SchwabAccount defaultSchwabAccount;

  public Long getUserId() { return userId; }
  public AppUser getUser() { return user; }
  public void setUser(AppUser user) { this.user = user; }
  public SchwabAccount getDefaultSchwabAccount() { return defaultSchwabAccount; }
  public void setDefaultSchwabAccount(SchwabAccount defaultSchwabAccount) { this.defaultSchwabAccount = defaultSchwabAccount; }
}
