package edu.uoc.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized database connection provider.
 *
 * <p>This class reads connection configuration from environment variables:</p>
 * <ul>
 *     <li>{@code DB_URL}</li>
 *     <li>{@code DB_USER}</li>
 *     <li>{@code DB_PASSWORD}</li>
 * </ul>
 *
 * <p>No credentials are stored in source code. This follows best practices
 * for secure configuration management.</p>
 *
 * <p>This implementation uses plain {@link DriverManager} and is intended
 * for educational and small-scale usage. In production systems, a connection
 * pool (e.g., HikariCP) should be used instead.</p>
 *
 * @since 1.0
 */
public final class Database {

    /**
     * Private constructor to prevent instantiation.
     * This is a pure utility class.
     */
    private Database() {
    }

    /**
     * Obtains a new JDBC connection using environment variables.
     *
     * @return a new {@link Connection}
     * @throws SQLException           if the connection attempt fails
     * @throws IllegalStateException  if required environment variables are missing
     */
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
