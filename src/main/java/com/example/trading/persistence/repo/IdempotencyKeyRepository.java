package com.example.trading.persistence.repo;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.IdempotencyKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
  Optional<IdempotencyKey> findByUserAndKeyHash(AppUser user, String keyHash);
}
