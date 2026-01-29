package com.example.trading.service;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.SchwabAccount;
import com.example.trading.persistence.entity.UserSettings;
import com.example.trading.persistence.repo.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsService {
  private final UserSettingsRepository repo;

  public UserSettingsService(UserSettingsRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public UserSettings getOrCreate(AppUser user) {
    return repo.findById(user.getId()).orElseGet(() -> {
      UserSettings s = new UserSettings();
      s.setUser(user);
      return repo.save(s);
    });
  }

  @Transactional
  public void setDefaultAccount(AppUser user, SchwabAccount account) {
    UserSettings s = getOrCreate(user);
    s.setDefaultSchwabAccount(account);
    repo.save(s);
  }

  @Transactional
  public void clearDefaultAccount(AppUser user) {
    UserSettings s = getOrCreate(user);
    s.setDefaultSchwabAccount(null);
    repo.save(s);
  }
}
