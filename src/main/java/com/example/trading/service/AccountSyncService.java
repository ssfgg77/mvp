package com.example.trading.service;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.SchwabAccount;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.schwab.SchwabClient;
import com.example.trading.schwab.dto.SchwabAccountDto;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sync Schwab accounts into local DB (upsert by (user, schwab_account_ref)).
 *
 * Triggered after first connect (e.g., on dashboard load) and can be reused elsewhere.
 * - If default account is not set:
 *   - auto-set if exactly one account exists
 *   - otherwise require user selection in Settings
 */
@Service
public class AccountSyncService {
  private static final Logger log = LoggerFactory.getLogger(AccountSyncService.class);

  private final SchwabTokenService tokens;
  private final SchwabClient schwab;
  private final SchwabAccountRepository accounts;
  private final UserSettingsService settings;

  public AccountSyncService(
      SchwabTokenService tokens,
      SchwabClient schwab,
      SchwabAccountRepository accounts,
      UserSettingsService settings
  ) {
    this.tokens = tokens;
    this.schwab = schwab;
    this.accounts = accounts;
    this.settings = settings;
  }

  /**
   * Sync accounts only if the user has no accounts yet in DB.
   * Returns the user's accounts (post-sync).
   */
  @Transactional
  public List<SchwabAccount> syncIfEmpty(AppUser user, Authentication auth) {
    List<SchwabAccount> existing = accounts.findAllByUser(user);
    if (!existing.isEmpty()) {
      ensureDefaultAccount(user, existing);
      return existing;
    }
    return syncFromSchwab(user, auth);
  }

  /**
   * Force sync from Schwab and upsert into DB.
   */
  @Transactional
  public List<SchwabAccount> syncFromSchwab(AppUser user, Authentication auth) {
    String accessToken = tokens.requireAccessToken(auth);
    List<SchwabAccountDto> remote = schwab.getAccounts(accessToken);

    List<SchwabAccount> saved = new ArrayList<>();
    for (SchwabAccountDto dto : remote) {
      SchwabAccount acc = accounts.findByUserAndSchwabAccountRef(user, dto.schwabAccountRef())
          .orElseGet(() -> {
            SchwabAccount a = new SchwabAccount();
            a.setUser(user);
            a.setSchwabAccountRef(dto.schwabAccountRef());
            return a;
          });

      acc.setAccountType(dto.accountType());
      acc.setNickname(dto.nickname());
      saved.add(accounts.save(acc));
    }

    ensureDefaultAccount(user, saved);
    log.info("Synced {} Schwab accounts for user={}", saved.size(), user.getUsername());
    return saved;
  }

  private void ensureDefaultAccount(AppUser user, List<SchwabAccount> userAccounts) {
    var s = settings.getOrCreate(user);
    if (s.getDefaultSchwabAccount() != null) return;
    if (userAccounts.size() == 1) {
      settings.setDefaultAccount(user, userAccounts.get(0));
      log.info("Auto-set default Schwab account for user={} accountId={}",
          user.getUsername(), userAccounts.get(0).getId());
    }
  }
}
