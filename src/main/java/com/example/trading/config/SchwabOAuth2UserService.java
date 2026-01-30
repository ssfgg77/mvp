package com.example.trading.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class SchwabOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
  private static final String SCHWAB_REGISTRATION_ID = "schwab";
  private static final String USERNAME_ATTRIBUTE = "username";

  private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    if (!SCHWAB_REGISTRATION_ID.equals(userRequest.getClientRegistration().getRegistrationId())) {
      return delegate.loadUser(userRequest);
    }

    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
    String username = currentAuth != null ? currentAuth.getName() : null;
    if (username == null || username.isBlank()) {
      username = SCHWAB_REGISTRATION_ID;
    }

    Map<String, Object> attributes = new HashMap<>();
    attributes.put(USERNAME_ATTRIBUTE, username);

    var authorities = Stream.concat(
        currentAuth == null ? Stream.empty() : currentAuth.getAuthorities().stream(),
        Stream.of(new SimpleGrantedAuthority("ROLE_USER"))
    ).filter(Objects::nonNull).distinct().toList();

    return new DefaultOAuth2User(authorities, attributes, USERNAME_ATTRIBUTE);
  }
}
