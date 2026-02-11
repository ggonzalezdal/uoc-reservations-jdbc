package edu.uoc.dao;

import edu.uoc.db.Database;
import edu.uoc.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Customer}.
 *
 * <p>Responsible for persistence operations related to the
 * {@code customers} table.</p>
 *
 * <p>This DAO follows a simple JDBC pattern:</p>
 * <ul>
 *     <li>Opens a connection via {@link Database#getConnection()}</li>
 *     <li>Uses {@link PreparedStatement} for parameter binding</li>
 *     <li>Maps {@link ResultSet} rows into {@link Customer} objects</li>
 * </ul>
 *
 * <p>Transaction-aware methods (e.g., {@link #existsById(Connection, long)})
 * accept an external {@link Connection} and do not open or close it.</p>
 *
 * @since 1.0
 */
public class CustomerDao {

    /**
     * Retrieves all customers ordered by their identifier.
     *
     * @return list of all customers (empty list if none exist)
     * @throws RuntimeException if a database error occurs
     */
    public List<Customer> findAll() {
        String sql = """
            SELECT customer_id, full_name, phone, email, created_at
            FROM customers
            ORDER BY customer_id
            """;

        List<Customer> customers = new ArrayList<>();

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                customers.add(mapRow(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching customers", e);
        }

        return customers;
    }

    /**
     * Inserts a new customer.
     *
     * <p>The database generates the {@code customer_id}, which is returned
     * using PostgreSQL's {@code RETURNING} clause.</p>
     *
     * @param fullName customer's full name (required)
     * @param phone    customer's phone number (required)
     * @param email    customer's email (nullable)
     * @return generated customer ID
     * @throws RuntimeException if insertion fails
     */
    public long insert(String fullName, String phone, String email) {
        String sql = """
            INSERT INTO customers (full_name, phone, email)
            VALUES (?, ?, ?)
            RETURNING customer_id
            """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, fullName);
            stmt.setString(2, phone);
            stmt.setString(3, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("customer_id");
                } else {
                    throw new RuntimeException("Insert failed, no ID returned");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error inserting customer", e);
        }
    }

    /**
     * Retrieves a customer by its identifier.
     *
     * @param id customer ID
     * @return optional containing the customer if found, otherwise empty
     * @throws RuntimeException if a database error occurs
     */
    public Optional<Customer> findById(long id) {
        String sql = """
            SELECT customer_id, full_name, phone, email, created_at
            FROM customers
            WHERE customer_id = ?
            """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                } else {
                    return Optional.empty();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching customer with id " + id, e);
        }
    }

    /**
     * Checks whether a customer exists by ID.
     *
     * <p>This method is transaction-aware and must be called
     * within an existing transaction context.</p>
     *
     * <p>It does not open or close the provided connection.</p>
     *
     * @param conn       existing database connection
     * @param customerId customer ID to check
     * @return true if the customer exists, false otherwise
     * @throws RuntimeException if a database error occurs
     */
    public boolean existsById(Connection conn, long customerId) {
        String sql = """
            SELECT 1
            FROM customers
            WHERE customer_id = ?
            LIMIT 1
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error checking existence of customer " + customerId,
                    e
            );
        }
    }

    // -------------------------------------------------------------------------
    // Internal mapping helper
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link ResultSet} row to a {@link Customer} object.
     *
     * @param rs result set positioned at a valid row
     * @return mapped customer instance
     */
    private Customer mapRow(ResultSet rs) throws Exception {
        return new Customer(
                rs.getLong("customer_id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}
