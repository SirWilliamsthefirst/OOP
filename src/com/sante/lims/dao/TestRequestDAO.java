package com.sante.lims.dao;

import com.sante.lims.model.TestRequest;
import com.sante.lims.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestRequestDAO {

    private static final String BASE_SELECT = """
        SELECT tr.*, u.full_name AS customer_name, tt.name AS test_type_name
        FROM test_requests tr
        JOIN users u  ON u.id  = tr.customer_id
        JOIN test_types tt ON tt.id = tr.test_type_id
        """;

    public List<TestRequest> findAll() {
        List<TestRequest> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + "ORDER BY tr.created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<TestRequest> findByCustomer(UUID customerId) {
        List<TestRequest> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE tr.customer_id = ? ORDER BY tr.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Optional<TestRequest> create(UUID customerId, UUID testTypeId) {
        String sql = """
            INSERT INTO test_requests (customer_id, test_type_id, payment_status, status, created_at, updated_at)
            VALUES (?, ?, 'UNPAID', 'PENDING', NOW(), NOW()) RETURNING id
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, customerId);
            ps.setObject(2, testTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID id = rs.getObject("id", UUID.class);
                    return findById(id);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public Optional<TestRequest> findById(UUID id) {
        String sql = BASE_SELECT + "WHERE tr.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public boolean markPaid(UUID requestId, UUID markedBy) {
        String sql = """
            UPDATE test_requests
            SET payment_status = 'PAID', payment_marked_by = ?, payment_marked_at = NOW(), updated_at = NOW()
            WHERE id = ? AND payment_status = 'UNPAID'
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, markedBy);
            ps.setObject(2, requestId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateStatus(UUID requestId, String newStatus) {
        String sql = "UPDATE test_requests SET status = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setObject(2, requestId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean setResultReadyAt(UUID requestId, LocalDateTime readyAt) {
        String sql = "UPDATE test_requests SET result_ready_at = ?, status = 'COMPLETED', updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(readyAt));
            ps.setObject(2, requestId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private TestRequest mapRow(ResultSet rs) throws SQLException {
        TestRequest r = new TestRequest();
        r.setId(rs.getObject("id", UUID.class));
        r.setCustomerId(rs.getObject("customer_id", UUID.class));
        r.setCustomerName(rs.getString("customer_name"));
        r.setTestTypeId(rs.getObject("test_type_id", UUID.class));
        r.setTestTypeName(rs.getString("test_type_name"));
        r.setPaymentStatus(TestRequest.PaymentStatus.valueOf(rs.getString("payment_status")));
        r.setPaymentMarkedBy(rs.getObject("payment_marked_by", UUID.class));
        r.setStatus(TestRequest.Status.valueOf(rs.getString("status")));
        r.setNotes(rs.getString("notes"));
        r.setPaymentMarkedAt(rs.getTimestamp("payment_marked_at") != null ? rs.getTimestamp("payment_marked_at").toLocalDateTime() : null);
        r.setResultReadyAt(rs.getTimestamp("result_ready_at") != null ? rs.getTimestamp("result_ready_at").toLocalDateTime() : null);
        r.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        r.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return r;
    }
}
