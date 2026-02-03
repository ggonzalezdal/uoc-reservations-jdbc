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

public class CustomerDao {

    public List<Customer> findAll() {
        String sql = """
            SELECT customer_id, full_name, email, created_at
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
                customers.add(new Customer(
                        rs.getLong("customer_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching customers", e);
        }

        return customers;
    }

    public long insert(String fullName, String email) {
        String sql = """
        INSERT INTO customers (full_name, email)
        VALUES (?, ?)
        RETURNING customer_id
        """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, fullName);
            stmt.setString(2, email);

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

    public Optional<Customer> findById(long id) {
        String sql = """
        SELECT customer_id, full_name, email, created_at
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
                    Customer customer = new Customer(
                            rs.getLong("customer_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getObject("created_at", OffsetDateTime.class)
                    );
                    return Optional.of(customer);
                } else {
                    return Optional.empty();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching customer with id " + id, e);
        }
    }


}
