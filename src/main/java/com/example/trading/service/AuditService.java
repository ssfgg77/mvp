package com.example.trading.service;

import com.example.trading.persistence.entity.AuditLog;
import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditLogRepository audit;

  public AuditService(AuditLogRepository audit) {
    this.audit = audit;
  }

  public void record(AppUser user, String action, String entityType, String entityId, String detail) {
    AuditLog log = new AuditLog();
    log.setUser(user);
    log.setAction(action);
    log.setEntityType(entityType);
    log.setEntityId(entityId);
    log.setDetail(detail);
    audit.save(log);
  }
}
