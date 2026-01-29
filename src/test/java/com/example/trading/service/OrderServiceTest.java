package com.example.trading.service;

import com.example.trading.api.dto.PlaceOrderRequest;
import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.SchwabAccount;
import com.example.trading.persistence.entity.UserSettings;
import com.example.trading.persistence.repo.AppUserRepository;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.persistence.repo.TradeOrderRepository;
import com.example.trading.persistence.repo.UserSettingsRepository;
import com.example.trading.schwab.SchwabClient;
import com.example.trading.schwab.dto.CancelOrderResultDto;
import com.example.trading.schwab.dto.PlaceOrderResultDto;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class OrderServiceTest {

  @Autowired private OrderService orderService;
  @Autowired private AppUserRepository users;
  @Autowired private SchwabAccountRepository accounts;
  @Autowired private UserSettingsRepository settings;
  @Autowired private TradeOrderRepository orders;
  @Autowired private SchwabClient schwab;

  private Authentication auth;
  private SchwabAccount account;

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    SchwabClient schwabClient() {
      return mock(SchwabClient.class);
    }

    @Bean
    @Primary
    OAuth2AuthorizedClientManager testAuthorizedClientManager() {
      ClientRegistration registration = ClientRegistration.withRegistrationId("schwab")
          .clientId("test-client")
          .clientSecret("test-secret")
          .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
          .redirectUri("http://localhost")
          .authorizationUri("https://example.test/oauth/authorize")
          .tokenUri("https://example.test/oauth/token")
          .scope("openid")
          .build();

      OAuth2AccessToken token = new OAuth2AccessToken(
          BEARER,
          "token-123",
          Instant.now().minusSeconds(60),
          Instant.now().plusSeconds(3600)
      );

      return request -> new OAuth2AuthorizedClient(
          registration,
          request.getPrincipal().getName(),
          token
      );
    }
  }

  @BeforeEach
  void setup() {
    AppUser user = new AppUser();
    user.setUsername("trader");
    user.setEmail("trader@example.com");
    user.setPasswordHash("pw");
    user.setRoles(Set.of("USER"));
    user = users.save(user);

    account = new SchwabAccount();
    account.setUser(user);
    account.setSchwabAccountRef("hash-abc");
    account = accounts.save(account);

    UserSettings userSettings = new UserSettings();
    userSettings.setUser(user);
    userSettings.setDefaultSchwabAccount(account);
    settings.save(userSettings);

    auth = new UsernamePasswordAuthenticationToken(
        user.getUsername(),
        "N/A",
        List.of(new SimpleGrantedAuthority("USER"))
    );

    when(schwab.placeOrder(eq("token-123"), eq("hash-abc"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new PlaceOrderResultDto("schwab-1", "SUBMITTED"));
    when(schwab.cancelOrder(anyString(), anyString(), anyString()))
        .thenReturn(new CancelOrderResultDto("CANCEL_REQUESTED"));
  }

  @Test
  void idempotencySameKeySamePayloadReturnsExistingOrder() {
    PlaceOrderRequest req = new PlaceOrderRequest();
    req.symbol = "AAPL";
    req.side = "BUY";
    req.quantity = 10;
    req.orderType = "MARKET";

    var first = orderService.placeOrder("idem-1", req, auth);
    var second = orderService.placeOrder("idem-1", req, auth);

    assertThat(second.orderId).isEqualTo(first.orderId);
    assertThat(second.status).isEqualTo(first.status);
  }

  @Test
  void idempotencySameKeyDifferentPayloadThrows() {
    PlaceOrderRequest req = new PlaceOrderRequest();
    req.symbol = "AAPL";
    req.side = "BUY";
    req.quantity = 10;
    req.orderType = "MARKET";
    orderService.placeOrder("idem-2", req, auth);

    PlaceOrderRequest modified = new PlaceOrderRequest();
    modified.symbol = "AAPL";
    modified.side = "BUY";
    modified.quantity = 11;
    modified.orderType = "MARKET";

    assertThatThrownBy(() -> orderService.placeOrder("idem-2", modified, auth))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void cancelSetsCancelRequested() {
    PlaceOrderRequest req = new PlaceOrderRequest();
    req.symbol = "MSFT";
    req.side = "BUY";
    req.quantity = 5;
    req.orderType = "MARKET";

    var placed = orderService.placeOrder("idem-3", req, auth);
    orderService.cancelOrder(placed.orderId, auth);

    var updated = orders.findById(placed.orderId).orElseThrow();
    assertThat(updated.getStatus().name()).isEqualTo("CANCEL_REQUESTED");
  }
}
