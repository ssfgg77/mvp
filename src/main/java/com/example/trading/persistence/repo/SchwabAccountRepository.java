package com.example.trading.persistence.repo;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.SchwabAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchwabAccountRepository extends JpaRepository<SchwabAccount, Long> {
  List<SchwabAccount> findAllByUser(AppUser user);
  Optional<SchwabAccount> findByIdAndUser(Long id, AppUser user);
  Optional<SchwabAccount> findByUserAndSchwabAccountRef(AppUser user, String ref);
}
