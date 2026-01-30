package com.example.trading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      OAuth2AuthorizationRequestResolver authorizationRequestResolver
  ) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/register",
                "/login",
                "/css/**",
                "/js/**",
                "/images/**",
                "/actuator/health",
                "/oauth2/**",
                "/login/oauth2/**"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .permitAll()
        )
        .logout(logout -> logout.logoutUrl("/logout"))
        .oauth2Login(oauth -> oauth
            .authorizationEndpoint(endpoint -> endpoint
                .authorizationRequestResolver(authorizationRequestResolver)
            )
        );

    return http.build();
  }

  @Bean
  OAuth2AuthorizationRequestResolver authorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository
  ) {
    return new SchwabAuthorizationRequestResolver(clientRegistrationRepository);
  }
}
