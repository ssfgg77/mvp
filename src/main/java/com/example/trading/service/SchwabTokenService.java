package com.example.trading.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;

@Service
public class SchwabTokenService {

  private final OAuth2AuthorizedClientManager manager;

  public SchwabTokenService(OAuth2AuthorizedClientManager manager) {
    this.manager = manager;
  }

  public String requireAccessToken(Authentication authentication) {
    OAuth2AuthorizeRequest req = OAuth2AuthorizeRequest.withClientRegistrationId("schwab")
        .principal(authentication)
        .build();

    OAuth2AuthorizedClient client = manager.authorize(req);
    if (client == null || client.getAccessToken() == null) {
      throw new IllegalStateException("Schwab not connected or token unavailable");
    }
    return client.getAccessToken().getTokenValue();
  }
}
