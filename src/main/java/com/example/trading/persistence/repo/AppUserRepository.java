package com.example.trading.persistence.repo;

import com.example.trading.persistence.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByUsername(String username);
  Optional<AppUser> findByEmail(String email);
  Optional<AppUser> findByUsernameOrEmail(String username, String email);
}
