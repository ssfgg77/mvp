package com.example.trading.web;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AccountsController {

  private final CurrentUserService currentUser;
  private final SchwabAccountRepository accounts;

  public AccountsController(CurrentUserService currentUser, SchwabAccountRepository accounts) {
    this.currentUser = currentUser;
    this.accounts = accounts;
  }

  @GetMapping("/accounts")
  public String accounts(Authentication auth, Model model) {
    AppUser user = currentUser.requireUser(auth);
    model.addAttribute("accounts", accounts.findAllByUser(user));
    return "accounts";
  }

  @GetMapping("/accounts/{accountId}")
  public String positions(@PathVariable long accountId, Authentication auth, Model model) {
    AppUser user = currentUser.requireUser(auth);
    var acc = accounts.findByIdAndUser(accountId, user).orElseThrow();
    model.addAttribute("account", acc);
    return "positions";
  }
}
