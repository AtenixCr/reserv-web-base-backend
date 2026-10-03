package com.tourism.business.audit.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

public final class AuditDataSource extends DelegatingDataSource {
    public AuditDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return prepare(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return prepare(super.getConnection(username, password));
    }

    private void context(Connection connection, AuditContext.Identity identity) throws SQLException {
        try (var statement = connection.prepareStatement("EXEC sys.sp_set_session_context @key=N'actorId', @value=?; EXEC sys.sp_set_session_context @key=N'requestId', @value=?;")) {
            if (identity == null || identity.actorId() == null) statement.setNull(1, Types.BIGINT);
            else statement.setLong(1, identity.actorId());
            if (identity == null) statement.setNull(2, Types.VARCHAR);
            else statement.setString(2, identity.requestId().toString());
            statement.execute();
        }
    }

    private Connection prepare(Connection connection) throws SQLException {
        try {
            context(connection, AuditContext.current());
        } catch (SQLException failure) {
            try { connection.abort(Runnable::run); } catch (SQLException cleanup) { failure.addSuppressed(cleanup); }
            try { connection.close(); } catch (SQLException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
        boolean[] closed = {false};
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
            if (method.getName().equals("close")) {
                if (closed[0]) return null;
                closed[0] = true;
                try {
                    context(connection, null);
                } catch (SQLException failure) {
                    try { connection.abort(Runnable::run); } catch (SQLException cleanup) { failure.addSuppressed(cleanup); }
                    throw failure;
                } finally {
                    connection.close();
                }
                return null;
            }
            if (method.getName().equals("isClosed") && closed[0]) return true;
            try {
                return method.invoke(connection, args);
            } catch (InvocationTargetException failure) {
                throw failure.getCause();
            }
        });
    }
}
