package com.example.trading.persistence.repo;

import com.example.trading.persistence.entity.AuditLog;
import com.example.trading.persistence.entity.AppUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findTop200ByUserOrderByCreatedAtDesc(AppUser user);
}
