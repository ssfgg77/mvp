package com.example.trading.api;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.service.CurrentUserService;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountsApiController {

  private final CurrentUserService currentUser;
  private final SchwabAccountRepository accounts;

  public AccountsApiController(CurrentUserService currentUser, SchwabAccountRepository accounts) {
    this.currentUser = currentUser;
    this.accounts = accounts;
  }

  @GetMapping
  public List<?> list(Authentication auth) {
    AppUser user = currentUser.requireUser(auth);
    return accounts.findAllByUser(user).stream().map(a -> Map.of(
        "id", a.getId(),
        "schwabAccountRef", a.getSchwabAccountRef(),
        "accountType", a.getAccountType(),
        "nickname", a.getNickname(),
        "cashBalance", a.getCashBalance(),
        "equityValue", a.getEquityValue(),
        "lastRefreshedAt", a.getLastRefreshedAt()
    )).toList();
  }
}
