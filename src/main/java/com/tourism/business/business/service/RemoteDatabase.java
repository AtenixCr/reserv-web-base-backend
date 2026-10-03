package com.tourism.business.business.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;

/** Explicit operator tool; never called by application startup. */
public final class RemoteDatabase {
    public static void requireTarget(String profile, String url) {
        if (!"remote".equals(profile) || url == null
                || !url.startsWith("jdbc:sqlserver://")
                || !url.contains(";databaseName=TourismDemo;")
                || url.split("(?i);databaseName=", -1).length != 2) {
            throw new IllegalArgumentException("Use remote profile and the dedicated TourismDemo database.");
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !"remote".equals(System.getenv("SPRING_PROFILES_ACTIVE")))
            throw new IllegalArgumentException("Use the remote profile and check, create or migrate.");
        String action = args[0];
        if (!java.util.List.of("check", "create", "migrate").contains(action))
            throw new IllegalArgumentException("Unknown action");
        String url = System.getenv("DB_URL");
        requireTarget(System.getenv("SPRING_PROFILES_ACTIVE"), url);
        String user = System.getenv("MIGRATION_USERNAME");
        String password = System.getenv("MIGRATION_PASSWORD");
        // Check can run before database creation and is read-only.
        String connectionUrl = (action.equals("check") || action.equals("create"))
            ? url.replace("databaseName=TourismDemo;", "databaseName=master;") : url;
        try (var connection = DriverManager.getConnection(connectionUrl, user, password)) {
            if (action.equals("create")) {
                String sql = Files.readString(Path.of("sql/ssms/01-crear-base.sql"), StandardCharsets.UTF_8);
                for (String batch : sql.split("(?im)^GO\\s*$")) {
                    if (!batch.isBlank()) try (var statement = connection.createStatement()) { statement.execute(batch); }
                }
                System.out.println("TourismDemo created. Run migrate.");
                return;
            }
            if (action.equals("check")) {
                try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                        "SELECT CONVERT(varchar(40),SERVERPROPERTY('ProductVersion')), "
                        + "HAS_PERMS_BY_NAME(NULL,NULL,'CREATE ANY DATABASE'), DB_ID(N'TourismDemo')")) {
                    rows.next();
                    System.out.println("SQL Server version=" + rows.getString(1)
                        + "; create_database=" + rows.getString(2) + "; target_id=" + rows.getString(3));
                }
                return;
            }
            if (!"TourismDemo".equals(connection.getCatalog()))
                throw new IllegalStateException("Only the dedicated TourismDemo database is allowed.");
            if (action.equals("migrate")) {
                var flyway = Flyway.configure().dataSource(url, user, password)
                    .cleanDisabled(true).validateMigrationNaming(true).load();
                flyway.migrate();
                flyway.validate();
                return;
            }
        }
    }
}
