package com.example.trading;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

  @Test
  void migrationsApplyCleanly() {
    Flyway flyway = Flyway.configure()
        .dataSource(
            "jdbc:h2:mem:flywaytest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
            "sa",
            ""
        )
        .locations("classpath:db/migration")
        .load();

    var result = flyway.migrate();
    assertThat(result.migrations).isNotEmpty();
  }
}
