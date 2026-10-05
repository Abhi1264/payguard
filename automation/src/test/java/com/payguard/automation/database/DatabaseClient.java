package com.payguard.automation.database;

import com.payguard.automation.config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public final class DatabaseClient {

    public String paymentStatus(UUID id) {
        return queryString("select status from paymentsvc.payments where id = ?", id);
    }

    public String orderStatus(UUID id) {
        return queryString("select status from ordersvc.orders where id = ?", id);
    }

    public Instant paymentAuthorizedAt(UUID id) {
        return queryInstant("select authorized_at from paymentsvc.payments where id = ?", id);
    }

    public String describePayment(UUID id) {
        return describe(
                "select status, provider_transaction_id, authorized_at from paymentsvc.payments where id = ?",
                id,
                "payment");
    }

    public String describeOrder(UUID id) {
        return describe("select status, amount, currency from ordersvc.orders where id = ?", id, "order");
    }

    private String describe(String sql, UUID id, String label) {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return label + " " + id + " not found";
                }
                StringBuilder text = new StringBuilder(label).append(' ').append(id);
                for (int column = 1; column <= rows.getMetaData().getColumnCount(); column++) {
                    text.append('\n')
                            .append(rows.getMetaData().getColumnLabel(column))
                            .append(": ")
                            .append(rows.getString(column));
                }
                return text.toString();
            }
        } catch (SQLException ex) {
            return label + " lookup failed: " + ex.getMessage();
        }
    }

    private String queryString(String sql, UUID id) {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new AssertionError("no row for " + id);
                }
                return rows.getString(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(sql, ex);
        }
    }

    private Instant queryInstant(String sql, UUID id) {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new AssertionError("no row for " + id);
                }
                Timestamp timestamp = rows.getTimestamp(1);
                return timestamp == null ? null : timestamp.toInstant();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(sql, ex);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(Config.dbUrl(), Config.dbUsername(), Config.dbPassword());
    }
}
