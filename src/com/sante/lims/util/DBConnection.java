package com.sante.lims.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton JDBC connection utility.
 * Update DB_URL, USER, and PASSWORD to match your local PostgreSQL setup.
 */
public class DBConnection {

    private static final String DB_URL  = "jdbc:postgresql://localhost:5433"
            + "/sante_lims";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "cos101";

    private static Connection connection;

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
