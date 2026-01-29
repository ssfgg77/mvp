package com.example.trading.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_key",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_idem_user_keyhash", columnNames = {"user_id","key_hash"})
    })
public class IdempotencyKey {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_idem_user"))
  private AppUser user;

  @Column(name = "key_hash", nullable = false, length = 64)
  private String keyHash;

  @Column(name = "request_fingerprint", nullable = false, length = 128)
  private String requestFingerprint;

  @Column(name = "order_id", columnDefinition = "BINARY(16)", nullable = false)
  private UUID orderId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public AppUser getUser() { return user; }
  public void setUser(AppUser user) { this.user = user; }
  public String getKeyHash() { return keyHash; }
  public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
  public String getRequestFingerprint() { return requestFingerprint; }
  public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }
  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID orderId) { this.orderId = orderId; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
