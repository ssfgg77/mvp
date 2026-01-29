package com.example.trading.api;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.service.CurrentUserService;
import com.example.trading.service.UserSettingsService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

  private final OAuth2AuthorizedClientService authorizedClientService;
  private final CurrentUserService currentUser;
  private final UserSettingsService settings;

  public AuthApiController(
      OAuth2AuthorizedClientService authorizedClientService,
      CurrentUserService currentUser,
      UserSettingsService settings
  ) {
    this.authorizedClientService = authorizedClientService;
    this.currentUser = currentUser;
    this.settings = settings;
  }

  @GetMapping("/status")
  public Map<String, Object> status(Authentication auth) {
    var client = authorizedClientService.loadAuthorizedClient("schwab", auth.getName());
    boolean connected = client != null && client.getAccessToken() != null;
    return Map.of(
        "connected", connected,
        "principal", auth.getName(),
        "accessTokenExpiresAt", connected ? client.getAccessToken().getExpiresAt() : null
    );
  }

  @PostMapping("/disconnect")
  public ResponseEntity<?> disconnect(Authentication auth) {
    AppUser user = currentUser.requireUser(auth);
    authorizedClientService.removeAuthorizedClient("schwab", auth.getName());
    settings.clearDefaultAccount(user);
    return ResponseEntity.ok(Map.of("disconnected", true));
  }
}
