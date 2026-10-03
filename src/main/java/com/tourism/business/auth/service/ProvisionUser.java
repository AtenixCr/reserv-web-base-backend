package com.tourism.business.auth.service;

import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class ProvisionUser {
    public static void main(String[] args) throws Exception {
        var console = System.console();
        if (console == null) throw new IllegalStateException("An interactive terminal is required");
        var username = console.readLine("Username: ").trim();
        var name = console.readLine("Name: ").trim();
        var email = console.readLine("Email: ").trim();
        var phone = console.readLine("Phone: ").trim();
        var role = console.readLine("Role (ADMIN/STAFF): ").trim();
        var password = console.readPassword("Password: ");
        var confirmation = console.readPassword("Confirm password: ");
        try {
            if (username.isEmpty() || username.length() > 80 || name.length() < 2 || name.length() > 120
                || email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || phone.length() > 40 || !phone.matches("^\\+?[0-9 ()-]+$")
                || phone.replaceAll("\\D", "").length() < 7 || phone.replaceAll("\\D", "").length() > 15
                || !List.of("ADMIN", "STAFF").contains(role) || password.length < 12
                || !Arrays.equals(password, confirmation)) throw new IllegalArgumentException("Invalid user data or password confirmation");
            var hash = new BCryptPasswordEncoder(12).encode(java.nio.CharBuffer.wrap(password));
            try (var connection = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("MIGRATION_USERNAME"), System.getenv("MIGRATION_PASSWORD"))) {
                connection.setAutoCommit(false);
                try {
                    try (var statement = connection.prepareStatement("UPDATE dbo.AppUser WITH (UPDLOCK, SERIALIZABLE) SET passwordHash=?,name=?,email=?,phone=?,role=?,active=1 WHERE username=?; IF @@ROWCOUNT=0 INSERT dbo.AppUser(passwordHash,name,email,phone,role,username) VALUES (?,?,?,?,?,?)")) {
                        var fields = List.of(hash, name, email, phone, role, username);
                        for (int index = 0; index < 12; index++) statement.setString(index + 1, fields.get(index % 6));
                        statement.executeUpdate();
                    }
                    connection.commit();
                } catch (Exception failure) {
                    connection.rollback();
                    throw failure;
                }
            }
            console.printf("User saved. Restart the API to invalidate existing sessions.%n");
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
