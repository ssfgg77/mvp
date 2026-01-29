package com.example.trading.poller;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.SchwabAccount;
import com.example.trading.persistence.entity.UserSettings;
import com.example.trading.persistence.repo.AppUserRepository;
import com.example.trading.persistence.repo.SchwabAccountRepository;
import com.example.trading.persistence.repo.UserSettingsRepository;
import com.example.trading.schwab.SchwabClient;
import com.example.trading.schwab.dto.AccountSnapshotDto;
import com.example.trading.snapshot.PositionSnapshotStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

@SpringBootTest
class DefaultAccountRefreshJobTest {

  @Autowired private DefaultAccountRefreshJob job;
  @Autowired private AppUserRepository users;
  @Autowired private SchwabAccountRepository accounts;
  @Autowired private UserSettingsRepository settings;
  @Autowired private OAuth2AuthorizedClientService authorizedClients;
  @Autowired private PositionSnapshotStore snapshots;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private SchwabClient schwab;

  @TestConfiguration
  static class MockConfig {
    @Bean
    @Primary
    SchwabClient schwabClient() {
      return mock(SchwabClient.class);
    }
  }

  @BeforeEach
  void ensureOauth2Table() {
    jdbcTemplate.execute("""
        CREATE TABLE IF NOT EXISTS oauth2_authorized_client (
          client_registration_id VARCHAR(100) NOT NULL,
          principal_name VARCHAR(200) NOT NULL,
          access_token_type VARCHAR(100) NOT NULL,
          access_token_value BLOB NOT NULL,
          access_token_issued_at TIMESTAMP(6) NOT NULL,
          access_token_expires_at TIMESTAMP(6) NOT NULL,
          access_token_scopes VARCHAR(1000) DEFAULT NULL,
          refresh_token_value BLOB DEFAULT NULL,
          refresh_token_issued_at TIMESTAMP(6) DEFAULT NULL,
          created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
          PRIMARY KEY (client_registration_id, principal_name)
        )
        """);
  }

  @Test
  void refreshUpdatesBalancesAndSnapshot() {
    AppUser user = new AppUser();
    user.setUsername("alice");
    user.setEmail("alice@example.com");
    user.setPasswordHash("pw");
    user.setRoles(Set.of("USER"));
    user = users.save(user);

    SchwabAccount account = new SchwabAccount();
    account.setUser(user);
    account.setSchwabAccountRef("hash-123");
    account = accounts.save(account);

    UserSettings userSettings = new UserSettings();
    userSettings.setUser(user);
    userSettings.setDefaultSchwabAccount(account);
    settings.save(userSettings);

    ClientRegistration registration = ClientRegistration.withRegistrationId("schwab")
        .clientId("test-client")
        .clientSecret("test-secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://localhost")
        .authorizationUri("https://example.test/oauth/authorize")
        .tokenUri("https://example.test/oauth/token")
        .scope("openid")
        .build();

    OAuth2AccessToken accessToken = new OAuth2AccessToken(
        BEARER,
        "token-123",
        Instant.now().minusSeconds(60),
        Instant.now().plusSeconds(3600)
    );

    OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(
        registration,
        user.getUsername(),
        accessToken
    );

    authorizedClients.saveAuthorizedClient(
        client,
        new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            "N/A",
            List.of(new SimpleGrantedAuthority("USER"))
        )
    );

    AccountSnapshotDto snapshot = new AccountSnapshotDto(
        new BigDecimal("123.45"),
        new BigDecimal("999.99"),
        List.of(Map.of("symbol", "AAPL", "qty", 5))
    );
    when(schwab.getDefaultAccountSnapshot(eq("token-123"), eq("hash-123"))).thenReturn(snapshot);

    job.run();

    SchwabAccount refreshed = accounts.findById(account.getId()).orElseThrow();
    assertThat(refreshed.getCashBalance()).isEqualByComparingTo("123.45");
    assertThat(refreshed.getEquityValue()).isEqualByComparingTo("999.99");
    assertThat(refreshed.getLastRefreshedAt()).isNotNull();

    PositionSnapshotStore.Snapshot stored = snapshots.get(user.getId(), account.getId());
    assertThat(stored).isNotNull();
    assertThat(stored.positionsPayload()).isEqualTo(snapshot.positionsPayload());
  }
}
