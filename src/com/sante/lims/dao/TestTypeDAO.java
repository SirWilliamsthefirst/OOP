package com.sante.lims.dao;

import com.sante.lims.model.TestType;
import com.sante.lims.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestTypeDAO {

    public List<TestType> findAll() {
        List<TestType> list = new ArrayList<>();
        String sql = "SELECT * FROM test_types WHERE is_active = TRUE ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Optional<TestType> findById(UUID id) {
        String sql = "SELECT * FROM test_types WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public Optional<TestType> create(String name, String category, BigDecimal price,
                                      int tatHours, String resultFormat,
                                      String description, UUID createdBy) {
        String sql = """
            INSERT INTO test_types
              (name, category, price, tat_hours, result_format, description, is_active, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, TRUE, ?, NOW(), NOW()) RETURNING *
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, category.trim());
            ps.setBigDecimal(3, price);
            ps.setInt(4, tatHours);
            ps.setString(5, resultFormat);
            ps.setString(6, description);
            ps.setObject(7, createdBy);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public boolean deactivate(UUID id) {
        String sql = "UPDATE test_types SET is_active = FALSE, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private TestType mapRow(ResultSet rs) throws SQLException {
        TestType t = new TestType();
        t.setId(rs.getObject("id", UUID.class));
        t.setName(rs.getString("name"));
        t.setCategory(rs.getString("category"));
        t.setPrice(rs.getBigDecimal("price"));
        t.setTatHours(rs.getInt("tat_hours"));
        t.setResultFormat(TestType.ResultFormat.valueOf(rs.getString("result_format")));
        t.setDescription(rs.getString("description"));
        t.setActive(rs.getBoolean("is_active"));
        t.setCreatedBy(rs.getObject("created_by", UUID.class));
        t.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        t.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return t;
    }
}
