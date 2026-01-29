package com.example.trading.api;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.service.CurrentUserService;
import com.example.trading.snapshot.PositionSnapshotStore;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts/{accountId}/positions")
public class PositionsApiController {

  private final CurrentUserService currentUser;
  private final SchwabAccountRepository accounts;
  private final PositionSnapshotStore snapshots;

  public PositionsApiController(CurrentUserService currentUser, SchwabAccountRepository accounts, PositionSnapshotStore snapshots) {
    this.currentUser = currentUser;
    this.accounts = accounts;
    this.snapshots = snapshots;
  }

  @GetMapping
  public Map<String, Object> positions(@PathVariable long accountId, Authentication auth) {
    AppUser user = currentUser.requireUser(auth);
    var account = accounts.findByIdAndUser(accountId, user).orElseThrow();

    var snap = snapshots.get(user.getId(), account.getId());
    boolean stale = snapshots.isStale(snap);

    return Map.of(
        "accountId", account.getId(),
        "snapshotTimestamp", snap == null ? null : snap.timestamp(),
        "stale", stale,
        "positions", snap == null ? null : snap.positionsPayload()
    );
  }
}
