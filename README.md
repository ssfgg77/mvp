# Schwab Trading System - MVP

This repo implements the approved `design.md` + `plan.md` MVP:

- Spring Boot **4.0.2** (set in `pom.xml`)
- Java **25**
- Maven
- Spring MVC + Thymeleaf + **HTMX**
- Spring Security form login (session) + **Spring OAuth2 Client** (`oauth2Login`) for Schwab connect
- MySQL + JPA + Flyway
- Pollers:
  - Orders reconciliation: **every 5s**
  - Default-account balances/positions refresh: **every 30s**
- Positions served from an **in-memory snapshot store** (not persisted)
- Quotes are **live** and **not persisted**
- Cancel orders included in MVP

## Quick start

### 1) MySQL
Create a database (example):

```sql
CREATE DATABASE schwab_trading CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2) Configure environment variables
Copy `.env.example` to `.env` (or set env vars in your shell) and fill in values.

Required:
- `DB_URL`, `DB_USER`, `DB_PASS`
- `SCHWAB_CLIENT_ID`, `SCHWAB_CLIENT_SECRET`
- `SCHWAB_AUTH_URL`, `SCHWAB_TOKEN_URL`, `SCHWAB_API_BASE_URL`, `SCHWAB_SCOPES`

Optional (defaults shown):
- `APP_POLL_ORDERS_MS=5000`
- `APP_REFRESH_DEFAULT_ACCOUNT_MS=30000`
- `APP_SNAPSHOT_TTL_MS=300000`
- `APP_BASE_URL` (useful behind reverse proxies for stable redirect URIs)

### 3) Run
```bash
mvn spring-boot:run
```

App:
- Web UI: http://localhost:8080/

### 4) Connect flow (UI)
1) Register at `/register`
2) Log in at `/login`
3) Click **Connect Schwab** on the dashboard
4) Sync accounts (auto on first connect or via Settings)
5) Choose a default Schwab account in **Settings**

## Notes
- OAuth2 authorized client persistence uses Spring’s `JdbcOAuth2AuthorizedClientService` and the schema in
  `src/main/resources/db/migration/V2__oauth2_authorized_client.sql`.
- Orders are reconciled every ~5s for non-terminal statuses.
- Default-account balances/positions refresh every ~30s; positions are stored in `PositionSnapshotStore`.
- Positions API includes `snapshotTimestamp` and a `stale` flag if the snapshot exceeds the TTL.
- Quotes are live and not persisted.

## Default credentials
- Register a user at `/register`, then connect Schwab from the dashboard.
