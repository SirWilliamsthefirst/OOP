/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sante.lims.dao;

import com.sante.lims.util.DBConnection;

import java.sql.*;
import java.util.UUID;

public class SampleEventDAO {

    public boolean record(UUID requestId, String status, String notes, UUID recordedBy) {
        String sql = """
            INSERT INTO sample_events (request_id, status, notes, recorded_by, recorded_at)
            VALUES (?, ?, ?, ?, NOW())
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, requestId);
            ps.setString(2, status);
            ps.setString(3, notes);
            ps.setObject(4, recordedBy);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}
