/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sante.lims.dao;

import com.sante.lims.util.DBConnection;

import java.sql.*;
import java.util.Optional;

public class BankDetailsDAO {

    public record BankDetails(String bankName, String accountName, String accountNumber, String sortCode) {}

    public Optional<BankDetails> getActive() {
        String sql = "SELECT * FROM bank_details WHERE is_active = TRUE LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(new BankDetails(
                    rs.getString("bank_name"),
                    rs.getString("account_name"),
                    rs.getString("account_number"),
                    rs.getString("sort_code")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }
}
