package com.tourism.business;

import com.tourism.business.audit.service.AuditContext;
import com.tourism.business.audit.service.AuditDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class DatabaseFoundationTest {
    static String url = System.getenv("TEST_DB_URL");
    static String user = System.getenv("TEST_DB_USERNAME");
    static String password = System.getenv("TEST_DB_PASSWORD");

    static Connection connection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @BeforeAll
    static void migrate() throws Exception {
        try (var connection = connection()) {
            assertEquals("TourismIntegrationTest", connection.getCatalog());
        }
        var flyway = Flyway.configure().dataSource(url, user, password).cleanDisabled(true).load();
        flyway.migrate();
        flyway.validate();
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) { statement.execute(sql); }
    }

    static long number(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    @Test
    void pa16MultiRowInsertUpdateDeleteAndNoOp() throws Exception {
        try (var connection = connection()) {
            connection.setAutoCommit(false);
            try {
                long baseline = number(connection, "SELECT COUNT(*) FROM dbo.AuditLog WHERE entity='ClosedDate'");
                execute(connection, "INSERT dbo.ClosedDate(visitDate,reason) VALUES ('2080-01-01','private one'),('2080-01-02','private two')");
                assertEquals(baseline + 2, number(connection, "SELECT COUNT(*) FROM dbo.AuditLog WHERE entity='ClosedDate'"));
                execute(connection, "UPDATE dbo.ClosedDate SET reason=reason WHERE visitDate BETWEEN '2080-01-01' AND '2080-01-02'");
                assertEquals(baseline + 2, number(connection, "SELECT COUNT(*) FROM dbo.AuditLog WHERE entity='ClosedDate'"));
                execute(connection, "UPDATE dbo.ClosedDate SET reason='changed private text' WHERE visitDate BETWEEN '2080-01-01' AND '2080-01-02'");
                assertEquals(baseline + 4, number(connection, "SELECT COUNT(*) FROM dbo.AuditLog WHERE entity='ClosedDate'"));
                execute(connection, "UPDATE dbo.ClosedDate SET reason='Changed private text ' WHERE visitDate BETWEEN '2080-01-01' AND '2080-01-02'");
                assertEquals(baseline + 6, number(connection, "SELECT COUNT(*) FROM dbo.AuditLog WHERE entity='ClosedDate'"));
                execute(connection, "DELETE dbo.ClosedDate WHERE visitDate BETWEEN '2080-01-01' AND '2080-01-02'");
                assertEquals(baseline + 8, number(connection, "SELECT COUNT(*) FROM dbo.AuditLog WHERE entity='ClosedDate'"));
                assertEquals(0, number(connection, "SELECT COUNT(*) FROM dbo.AuditLog WHERE beforeData LIKE '%private%' OR afterData LIKE '%private%'"));
                assertEquals(0, number(connection, "SELECT COUNT(*) FROM dbo.AuditLog WHERE entity='ClosedDate' AND origin <> 'EXTERNAL'"));
            } finally { connection.rollback(); }
        }
    }

    @Test
    void pa16PooledConnectionClearsActorAndRequest() throws Exception {
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(1);
        try (var pool = new HikariDataSource(config)) {
            var audited = new AuditDataSource(pool);
            long session;
            try (var context = AuditContext.open(765L, UUID.randomUUID()); var connection = audited.getConnection()) {
                session = number(connection, "SELECT @@SPID");
                assertEquals(765, number(connection, "SELECT CONVERT(bigint,SESSION_CONTEXT(N'actorId'))"));
            }
            try (var raw = pool.getConnection()) {
                assertEquals(session, number(raw, "SELECT @@SPID"));
                assertEquals(1, number(raw, "SELECT CASE WHEN SESSION_CONTEXT(N'actorId') IS NULL AND SESSION_CONTEXT(N'requestId') IS NULL THEN 1 ELSE 0 END"));
            }
            try (var connection = audited.getConnection()) {
                assertEquals(1, number(connection, "SELECT CASE WHEN SESSION_CONTEXT(N'actorId') IS NULL AND SESSION_CONTEXT(N'requestId') IS NULL THEN 1 ELSE 0 END"));
            }
        }
    }

    @Test
    void pa16ApplicationContextIsRecordedAndSecretsAreExcluded() throws Exception {
        try (var connection = connection()) {
            connection.setAutoCommit(false);
            try {
                execute(connection, "INSERT dbo.AppUser(username,passwordHash,name,email,phone,role) VALUES ('audit.test','secret-hash','Private Name','private@example.invalid','00000000','ADMIN')");
                long actor = number(connection, "SELECT id FROM dbo.AppUser WHERE username='audit.test'");
                execute(connection, "EXEC sys.sp_set_session_context N'actorId', " + actor + "; EXEC sys.sp_set_session_context N'requestId', '" + UUID.randomUUID() + "'");
                execute(connection, "INSERT dbo.ClosedDate(visitDate,reason) VALUES ('2081-01-01','private reason')");
                assertEquals(actor, number(connection, "SELECT actorId FROM dbo.AuditLog WHERE entity='ClosedDate' AND origin='APPLICATION'"));
                assertEquals(0, number(connection, "SELECT COUNT(*) FROM dbo.AuditLog WHERE afterData LIKE '%secret-hash%' OR afterData LIKE '%private@example%' OR afterData LIKE '%Private Name%'"));
            } finally { connection.rollback(); }
        }
    }

    @Test
    void pa20SchemaIncludesRequiredEntitiesWithoutAutomaticSeed() throws Exception {
        try (var connection = connection()) {
            assertEquals(23, number(connection, "SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo') AND name <> 'flyway_schema_history'"));
            assertEquals(0, number(connection, "SELECT COUNT(*) FROM dbo.AppUser WHERE NOT (username LIKE 'admission.concurrent.%' AND active=0 AND passwordHash='unusable')"));
            assertEquals(0, number(connection, "SELECT COUNT(*) FROM dbo.BusinessSettings"));
            assertEquals(0, number(connection, "SELECT COUNT(*) FROM sys.triggers WHERE parent_id IN (OBJECT_ID('dbo.AuditLog'),OBJECT_ID('dbo.ActivityEvent'))"));
        }
    }

    @Test
    void pa20SeparateSeedAndImmutableHistoricalClosure() throws Exception {
        try (var connection = connection()) {
            connection.setAutoCommit(false);
            try {
                var hash = new BCryptPasswordEncoder(12).encode("TestAdmin!2026");
                try (var statement = connection.prepareStatement("INSERT dbo.AppUser(username,passwordHash,name,email,phone,role) VALUES ('admin.test',?,'Demo Administrator','admin@example.invalid','00000000','ADMIN')")) {
                    statement.setString(1, hash);
                    statement.executeUpdate();
                }
                var sql = Files.readString(Path.of("src/test/resources/sql/integration-seed.sql"));
                execute(connection, "EXEC sys.sp_set_session_context N'environment', N'integration-test'");
                for (var batch : sql.split("(?im)^GO\\s*$")) if (!batch.isBlank()) execute(connection, batch);
                assertEquals(17000, number(connection, "SELECT total FROM dbo.Reservation WHERE name=N'Visitante de prueba'"));
                assertEquals(63000, number(connection, "SELECT expectedCash FROM dbo.CashClosure WHERE actorId=(SELECT id FROM dbo.AppUser WHERE username='admin.test')"));
                assertEquals(-500, number(connection, "SELECT difference FROM dbo.CashClosure WHERE actorId=(SELECT id FROM dbo.AppUser WHERE username='admin.test')"));
                assertEquals(83000, number(connection, "SELECT operatingResult FROM dbo.CashClosure WHERE actorId=(SELECT id FROM dbo.AppUser WHERE username='admin.test')"));
                assertEquals(3, number(connection, "SELECT COUNT(DISTINCT method) FROM dbo.Payment"));
                assertEquals(1, number(connection, "SELECT COUNT(*) FROM dbo.Adjustment WHERE actorId=(SELECT id FROM dbo.AppUser WHERE username='admin.test')"));
                assertThrows(SQLException.class, () -> execute(connection, "UPDATE dbo.CashClosure SET countedCash=countedCash+1,difference=difference+1"));
            } finally { connection.rollback(); }
        }
    }

    @Test
    void sqlConstraintsRejectInvalidQuantitiesAndDuplicateDates() throws Exception {
        try (var connection = connection()) {
            connection.setAutoCommit(false);
            try {
                execute(connection, "INSERT dbo.VisitDay(visitDate) VALUES ('2082-01-01')");
                assertThrows(SQLException.class, () -> execute(connection, "INSERT dbo.VisitDay(visitDate) VALUES ('2082-01-01')"));
                assertThrows(SQLException.class, () -> execute(connection, "INSERT dbo.VisitDay(visitDate,capacityOverride) VALUES ('2082-01-02',-1)"));
                assertThrows(SQLException.class, () -> execute(connection, "INSERT dbo.ExchangeRate(currency,rate,updatedAt) VALUES ('USD',0,SYSUTCDATETIME())"));
            } finally { connection.rollback(); }
        }
    }
}
