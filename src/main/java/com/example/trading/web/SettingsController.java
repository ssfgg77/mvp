package com.example.trading.web;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.service.CurrentUserService;
import com.example.trading.service.UserSettingsService;
import com.example.trading.service.AccountSyncService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class SettingsController {

  private final CurrentUserService currentUser;
  private final SchwabAccountRepository accounts;
  private final UserSettingsService settings;
  private final AccountSyncService accountSync;

  public SettingsController(CurrentUserService currentUser, SchwabAccountRepository accounts, UserSettingsService settings) {
    this.currentUser = currentUser;
    this.accounts = accounts;
    this.settings = settings;
  }

  @GetMapping("/settings")
  public String settingsPage(Authentication auth, @RequestParam(required = false) String chooseDefault, Model model) {
    AppUser user = currentUser.requireUser(auth);
    model.addAttribute("accounts", accounts.findAllByUser(user));
    model.addAttribute("chooseDefault", model.asMap().getOrDefault("chooseDefault", null));
    model.addAttribute("settings", settings.getOrCreate(user));
    model.addAttribute("chooseDefault", chooseDefault);
    return "settings";
  }

  @PostMapping("/settings/default-account")
  public String setDefault(@RequestParam long accountId, Authentication auth) {
    AppUser user = currentUser.requireUser(auth);
    var acc = accounts.findByIdAndUser(accountId, user).orElseThrow();
    settings.setDefaultAccount(user, acc);
    return "redirect:/settings?saved";
  }


@PostMapping("/settings/sync-accounts")
public String syncAccounts(Authentication auth) {
  AppUser user = currentUser.requireUser(auth);
  try {
    accountSync.syncFromSchwab(user, auth);
  } catch (UnsupportedOperationException e) {
    return "redirect:/settings?syncError=notImplemented";
  } catch (Exception e) {
    return "redirect:/settings?syncError=failed";
  }
  return "redirect:/settings?synced";
}

}
