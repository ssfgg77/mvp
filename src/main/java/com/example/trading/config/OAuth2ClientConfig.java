package com.example.trading.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;

@Configuration
public class OAuth2ClientConfig {

  @Bean
  public OAuth2AuthorizedClientService authorizedClientService(
      DataSource dataSource,
      ClientRegistrationRepository clientRegistrationRepository
  ) {
    return new JdbcOAuth2AuthorizedClientService(dataSource, clientRegistrationRepository);
  }

  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository registrations,
      OAuth2AuthorizedClientService authorizedClientService
  ) {
    OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
        .authorizationCode()
        .refreshToken()
        .build();

    var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClientService);
    manager.setAuthorizedClientProvider(provider);
    return manager;
  }
}
