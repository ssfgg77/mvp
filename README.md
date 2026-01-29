# Schwab Trading System — Ready-to-commit Skeleton

This repo is a **starter skeleton** that matches the approved `design.md` + `plan.md`:

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

### 3) Run
```bash
mvn spring-boot:run
```

App:
- Web UI: http://localhost:8080/
- OpenAPI UI (if enabled): http://localhost:8080/swagger-ui.html

## Notes
- The Schwab API calls are **stubbed** (`schwab/SchwabClient.java`). Implement HTTP calls per Schwab docs.
- OAuth2 authorized client persistence uses Spring’s `JdbcOAuth2AuthorizedClientService` and the schema in
  `src/main/resources/db/migration/V2__oauth2_authorized_client.sql`.
- Positions are not stored in DB; they come from `PositionSnapshotStore` populated by the 30s refresh job.

## Default credentials
- Register a user at `/register`, then connect Schwab from the dashboard.
