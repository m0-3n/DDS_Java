package org.example;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class test {
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/DDS_DB";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "toor";

    public static void main(String[] args) {
        // Test MySQL connection
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            // If connected successfully, print the database version
            ResultSet rs = stmt.executeQuery("SELECT VERSION()");
            if (rs.next()) {
                System.out.println("Connected to MySQL database. Version: " + rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}