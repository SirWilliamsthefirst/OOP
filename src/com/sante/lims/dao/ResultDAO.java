package com.sante.lims.dao;

import com.sante.lims.model.Result;
import com.sante.lims.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class ResultDAO {

    public Optional<Result> findByRequestId(UUID requestId) {
        String sql = "SELECT * FROM results WHERE request_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public Optional<Result> upload(UUID requestId, String resultFormat,
                                    BigDecimal numericValue, String textValue,
                                    String filePath, UUID uploadedBy) {
        // Upsert: if result already exists for request, update it
        String sql = """
            INSERT INTO results
              (request_id, result_format, numeric_value, text_value, file_path,
               is_validated, notification_sent, uploaded_by, uploaded_at, updated_at)
            VALUES (?, ?, ?, ?, ?, FALSE, FALSE, ?, NOW(), NOW())
            ON CONFLICT (request_id) DO UPDATE
              SET result_format = EXCLUDED.result_format,
                  numeric_value = EXCLUDED.numeric_value,
                  text_value    = EXCLUDED.text_value,
                  file_path     = EXCLUDED.file_path,
                  is_validated  = FALSE,
                  updated_at    = NOW()
            RETURNING *
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, requestId);
            ps.setString(2, resultFormat);
            ps.setBigDecimal(3, numericValue);
            ps.setString(4, textValue);
            ps.setString(5, filePath);
            ps.setObject(6, uploadedBy);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public boolean validate(UUID resultId, UUID validatedBy) {
        String sql = """
            UPDATE results
            SET is_validated = TRUE, validated_by = ?, validated_at = NOW(), updated_at = NOW()
            WHERE id = ? AND is_validated = FALSE
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, validatedBy);
            ps.setObject(2, resultId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean markNotificationSent(UUID resultId) {
        String sql = "UPDATE results SET notification_sent = TRUE, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, resultId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private Result mapRow(ResultSet rs) throws SQLException {
        Result r = new Result();
        r.setId(rs.getObject("id", UUID.class));
        r.setRequestId(rs.getObject("request_id", UUID.class));
        r.setResultFormat(Result.ResultFormat.valueOf(rs.getString("result_format")));
        r.setNumericValue(rs.getBigDecimal("numeric_value"));
        r.setTextValue(rs.getString("text_value"));
        r.setFilePath(rs.getString("file_path"));
        r.setValidated(rs.getBoolean("is_validated"));
        r.setValidatedBy(rs.getObject("validated_by", UUID.class));
        r.setNotificationSent(rs.getBoolean("notification_sent"));
        r.setUploadedBy(rs.getObject("uploaded_by", UUID.class));
        r.setValidatedAt(rs.getTimestamp("validated_at") != null ? rs.getTimestamp("validated_at").toLocalDateTime() : null);
        r.setUploadedAt(rs.getTimestamp("uploaded_at") != null ? rs.getTimestamp("uploaded_at").toLocalDateTime() : null);
        r.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return r;
    }
}
