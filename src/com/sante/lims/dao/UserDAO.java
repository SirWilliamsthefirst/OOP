package com.sante.lims.dao;

import com.sante.lims.model.User;
import com.sante.lims.service.AuthenticationException;
import com.sante.lims.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class UserDAO {

    private static final String SELECT_BY_EMAIL        = "SELECT * FROM users WHERE email = ?";
    private static final String SELECT_BY_ID           = "SELECT * FROM users WHERE id = ?";
    private static final String SELECT_BY_VERIFY_TOKEN = "SELECT * FROM users WHERE email = ? AND verify_token = ?";
    private static final String UPDATE_PASSWORD        = "UPDATE users SET password_hash = ?, force_pw_change = FALSE, updated_at = NOW() WHERE id = ?";
    private static final String VERIFY_EMAIL           = "UPDATE users SET email_verified = TRUE, verify_token = NULL, updated_at = NOW() WHERE id = ? AND email_verified = FALSE";

    public Optional<User> findByEmail(String email) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public Optional<User> findById(UUID id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public boolean updatePassword(UUID userId, String passwordHash) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_PASSWORD)) {
            ps.setString(1, passwordHash);
            ps.setObject(2, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public Optional<User> findByVerifyToken(String email, String token) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_VERIFY_TOKEN)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, token.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public boolean verifyEmail(UUID userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(VERIFY_EMAIL)) {
            ps.setObject(1, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public Optional<User> createCustomer(String fullName, String email,
                                          String passwordHash, String verifyToken) {
        String sql = """
            INSERT INTO users
              (full_name, email, password_hash, role, email_verified,
               verify_token, force_pw_change, created_at, updated_at)
            VALUES (?, ?, ?, 'CUSTOMER', FALSE, ?, FALSE, NOW(), NOW()) RETURNING *
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, passwordHash);
            ps.setString(4, verifyToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) return Optional.empty();
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void createStaffUser(String fullName, String email,
                                 User.Role role, UUID createdBy) throws AuthenticationException {
        if (findByEmail(email).isPresent()) {
            throw new AuthenticationException("An account with that email already exists.");
        }
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        String hash = BCrypt.hashpw(tempPassword, BCrypt.gensalt());
        String sql = """
            INSERT INTO users
              (full_name, email, password_hash, role, email_verified,
               force_pw_change, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, TRUE, TRUE, ?, NOW(), NOW())
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, hash);
            ps.setString(4, role.name());
            ps.setObject(5, createdBy);
            ps.executeUpdate();
            System.out.println("[UserDAO] Temp password for " + email + ": " + tempPassword);
        } catch (SQLException e) {
            throw new AuthenticationException("Failed to create account: " + e.getMessage());
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getObject("id", UUID.class));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        user.setEmailVerified(rs.getBoolean("email_verified"));
        user.setVerifyToken(rs.getString("verify_token"));
        user.setForcePwChange(rs.getBoolean("force_pw_change"));
        user.setCreatedBy(rs.getObject("created_by", UUID.class));
        user.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        user.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return user;
    }
}