package edu.uoc.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {

    private Database() { }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException(
                    "Missing environment variables. Please set DB_URL, DB_USER, DB_PASSWORD."
            );
        }

        return DriverManager.getConnection(url, user, password);
    }
}

