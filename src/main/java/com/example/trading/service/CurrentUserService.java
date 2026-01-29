package com.example.trading.service;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.repo.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
  private final AppUserRepository users;

  public CurrentUserService(AppUserRepository users) {
    this.users = users;
  }

  public AppUser requireUser(Authentication auth) {
    String username = auth.getName();
    return users.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB: " + username));
  }
}
