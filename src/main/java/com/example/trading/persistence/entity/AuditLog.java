package com.example.trading.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log",
    indexes = {
        @Index(name = "ix_audit_user_time", columnList = "user_id,created_at")
    })
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_audit_log_user"))
  private AppUser user;

  @Column(nullable = false, length = 64)
  private String action;

  @Column(name = "entity_type", length = 64)
  private String entityType;

  @Column(name = "entity_id", length = 64)
  private String entityId;

  @Column(length = 1024)
  private String detail;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public AppUser getUser() { return user; }
  public void setUser(AppUser user) { this.user = user; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getEntityType() { return entityType; }
  public void setEntityType(String entityType) { this.entityType = entityType; }
  public String getEntityId() { return entityId; }
  public void setEntityId(String entityId) { this.entityId = entityId; }
  public String getDetail() { return detail; }
  public void setDetail(String detail) { this.detail = detail; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
