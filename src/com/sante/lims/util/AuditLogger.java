package com.sante.lims.util;

import com.sante.lims.session.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Writes immutable entries to the audit_log table.
 * Call AuditLogger.log(...) after every significant user action.
 */
public class AuditLogger {

    private AuditLogger() {}

    /**
     * @param action     Short action code, e.g. "LOGIN", "CREATE_TEST", "MARK_PAID"
     * @param entityType Table name of the affected record, e.g. "test_requests"
     * @param entityId   PK of the affected record (may be null)
     * @param detail     Human-readable description
     */
    public static void log(String action, String entityType, UUID entityId, String detail) {
        UUID userId = null;
        if (SessionManager.getInstance().isLoggedIn()) {
            userId = SessionManager.getInstance().getCurrentUser().getId();
        }

        String sql = """
            INSERT INTO audit_log (user_id, action, entity_type, entity_id, detail)
            VALUES (?::uuid, ?, ?, ?::uuid, ?)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId != null ? userId.toString() : null);
            ps.setString(2, action);
            ps.setString(3, entityType);
            ps.setString(4, entityId != null ? entityId.toString() : null);
            ps.setString(5, detail);
            ps.executeUpdate();

        } catch (SQLException e) {
            // Audit failures should not crash the app – just print
            System.err.println("[AuditLogger] Failed to write audit entry: " + e.getMessage());
        }
    }

    /** Convenience overload without an entity. */
    public static void log(String action, String detail) {
        log(action, null, null, detail);
    }
}
