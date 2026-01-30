package com.example.trading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      OAuth2AuthorizationRequestResolver authorizationRequestResolver,
      OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService
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
                "/oauth/callback/**",
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
            .redirectionEndpoint(endpoint -> endpoint
                .baseUri("/oauth/callback")
            )
            .userInfoEndpoint(userInfo -> userInfo
                .userService(oauth2UserService)
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

  @Bean
  OAuth2UserService<OAuth2UserRequest, OAuth2User> schwabOAuth2UserService() {
    return new SchwabOAuth2UserService();
  }
}
