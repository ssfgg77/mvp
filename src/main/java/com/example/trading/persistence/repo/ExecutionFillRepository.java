package com.example.trading.persistence.repo;

import com.example.trading.persistence.entity.ExecutionFill;
import com.example.trading.persistence.entity.TradeOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionFillRepository extends JpaRepository<ExecutionFill, Long> {
  List<ExecutionFill> findAllByOrderOrderByExecTimeAsc(TradeOrder order);
  boolean existsByOrderAndExternalExecId(TradeOrder order, String externalExecId);
}
