package com.example.trading.poller;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.repo.AppUserRepository;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.persistence.repo.UserSettingsRepository;
import com.example.trading.snapshot.PositionSnapshotStore;
import com.example.trading.schwab.SchwabClient;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh balances + positions for each user's DEFAULT Schwab account every 30s.
 * Positions are stored in PositionSnapshotStore (not persisted).
 */
@Component
public class DefaultAccountRefreshJob {
  private static final Logger log = LoggerFactory.getLogger(DefaultAccountRefreshJob.class);

  private final AppUserRepository users;
  private final UserSettingsRepository settings;
  private final SchwabAccountRepository accounts;
  private final OAuth2AuthorizedClientService authorizedClients;
  private final SchwabClient schwab;
  private final PositionSnapshotStore snapshots;

  public DefaultAccountRefreshJob(
      AppUserRepository users,
      UserSettingsRepository settings,
      SchwabAccountRepository accounts,
      OAuth2AuthorizedClientService authorizedClients,
      SchwabClient schwab,
      PositionSnapshotStore snapshots,
      @Value("${app.refresh.default-account-ms:30000}") long intervalMs
  ) {
    this.users = users;
    this.settings = settings;
    this.accounts = accounts;
    this.authorizedClients = authorizedClients;
    this.schwab = schwab;
    this.snapshots = snapshots;
  }

  @Scheduled(fixedDelayString = "${app.refresh.default-account-ms:30000}")
  @Transactional
  public void run() {
    // Skeleton: iterate users and refresh their default account if connected
    for (AppUser user : users.findAll()) {
      var s = settings.findById(user.getId()).orElse(null);
      if (s == null || s.getDefaultSchwabAccount() == null) continue;

      // Check if user has Schwab connected (principalName = username)
      var client = authorizedClients.loadAuthorizedClient("schwab", user.getUsername());
      if (client == null || client.getAccessToken() == null) continue;

      String token = client.getAccessToken().getTokenValue();
      var acc = s.getDefaultSchwabAccount();

      try {
        var snap = schwab.getDefaultAccountSnapshot(token, acc.getSchwabAccountRef());
        acc.setCashBalance(snap.cashBalance());
        acc.setEquityValue(snap.equityValue());
        acc.setLastRefreshedAt(Instant.now());
        accounts.save(acc);
        snapshots.put(user.getId(), acc.getId(), snap.positionsPayload());

        log.debug("DefaultAccountRefreshJob refreshed user={} account={} at {}",
            user.getUsername(), acc.getId(), Instant.now());
      } catch (IllegalStateException e) {
        log.info("DefaultAccountRefreshJob failed user={} account={} reason={}",
            user.getUsername(), acc.getId(), e.getMessage());
      } catch (Exception e) {
        log.info("DefaultAccountRefreshJob failed user={} account={}",
            user.getUsername(), acc.getId(), e);
      }
    }
  }
}
