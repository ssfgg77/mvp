package com.example.trading.persistence.repo;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.TradeOrder;
import com.example.trading.persistence.entity.TradeOrder.Status;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, UUID> {
  Optional<TradeOrder> findByIdAndUser(UUID id, AppUser user);
  List<TradeOrder> findTop200ByUserOrderByCreatedAtDesc(AppUser user);
  List<TradeOrder> findTop200ByUserAndStatusOrderByCreatedAtDesc(AppUser user, Status status);

  @Query("select o from TradeOrder o where o.status in :statuses order by o.updatedAt asc")
  List<TradeOrder> findReconcilable(@Param("statuses") List<Status> statuses);
}
