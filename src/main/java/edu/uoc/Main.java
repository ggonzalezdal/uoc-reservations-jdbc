package edu.uoc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {

        // Use env vars so secrets are not committed to GitHub
        String url      = getenvOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/uoc_databases");
        String user     = getenvOrDefault("DB_USER", "postgres");
        String password = getenvOrDefault("DB_PASSWORD", ""); // set in env, not in code

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            System.out.println("Connected to database!");
            System.out.println("DB product   : " + conn.getMetaData().getDatabaseProductName());
            System.out.println("DB version   : " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("JDBC driver  : " + conn.getMetaData().getDriverName());
            System.out.println("Driver ver.  : " + conn.getMetaData().getDriverVersion());

        } catch (SQLException e) {
            System.err.println("Connection failed!");
            e.printStackTrace();
        }
    }

    private static String getenvOrDefault(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }
}
