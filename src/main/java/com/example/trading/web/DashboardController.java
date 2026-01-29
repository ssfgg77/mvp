package com.example.trading.web;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.service.CurrentUserService;
import com.example.trading.service.UserSettingsService;
import com.example.trading.service.AccountSyncService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

  private final CurrentUserService currentUser;
  private final OAuth2AuthorizedClientService authorizedClients;
  private final SchwabAccountRepository accounts;
  private final UserSettingsService settings;
  private final AccountSyncService accountSync;

  public DashboardController(
      CurrentUserService currentUser,
      OAuth2AuthorizedClientService authorizedClients,
      SchwabAccountRepository accounts,
      UserSettingsService settings,
      AccountSyncService accountSync
  ) {
    this.currentUser = currentUser;
    this.authorizedClients = authorizedClients;
    this.accounts = accounts;
    this.settings = settings;
    this.accountSync = accountSync;
  }

  @GetMapping("/")
  public String dashboard(Authentication auth, Model model) {
    AppUser user = currentUser.requireUser(auth);
    var client = authorizedClients.loadAuthorizedClient("schwab", auth.getName());
    boolean connected = client != null && client.getAccessToken() != null;

    var userSettings = settings.getOrCreate(user);

    // On first connect: populate Schwab accounts into DB (upsert) and ensure default selection UX.
    if (connected) {
      try {
        var syncedAccounts = accountSync.syncIfEmpty(user, auth);
        // If default account is not set and user has multiple accounts, force selection.
        if (userSettings.getDefaultSchwabAccount() == null && syncedAccounts.size() > 1) {
          return "redirect:/settings?chooseDefault=1";
        }
      } catch (UnsupportedOperationException e) {
        // Schwab client may not be implemented yet in skeleton; keep UI usable.
        model.addAttribute("error", "Schwab client not implemented yet (accounts sync skipped).");
      } catch (Exception e) {
        model.addAttribute("error", "Failed to sync Schwab accounts: " + e.getMessage());
      }
    }

    model.addAttribute("connected", connected);
    model.addAttribute("accounts", accounts.findAllByUser(user));
    model.addAttribute("settings", settings.getOrCreate(user));
    return "dashboard";
  }

  @GetMapping("/connect/schwab")
  public String connect() {
    return "redirect:/oauth2/authorization/schwab";
  }
}
