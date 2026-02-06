package edu.uoc.dao;

import edu.uoc.db.Database;
import edu.uoc.model.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TableDao {

    public List<Table> findAll() {
        String sql = """
            SELECT table_id, table_code, capacity, active
            FROM tables
            ORDER BY table_code
            """;

        List<Table> tables = new ArrayList<>();

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                tables.add(new Table(
                        rs.getLong("table_id"),
                        rs.getString("table_code"),
                        rs.getInt("capacity"),
                        rs.getBoolean("active")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching tables", e);
        }

        return tables;
    }

    public Optional<Table> findById(long id) {
        String sql = """
            SELECT table_id, table_code, capacity, active
            FROM tables
            WHERE table_id = ?
            """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Table(
                            rs.getLong("table_id"),
                            rs.getString("table_code"),
                            rs.getInt("capacity"),
                            rs.getBoolean("active")
                    ));
                }
                return Optional.empty();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching table with id " + id, e);
        }
    }

    public Optional<Table> findByCode(String code) {
        String sql = """
            SELECT table_id, table_code, capacity, active
            FROM tables
            WHERE table_code = ?
            """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, code);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Table(
                            rs.getLong("table_id"),
                            rs.getString("table_code"),
                            rs.getInt("capacity"),
                            rs.getBoolean("active")
                    ));
                }
                return Optional.empty();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching table with code " + code, e);
        }
    }
}

