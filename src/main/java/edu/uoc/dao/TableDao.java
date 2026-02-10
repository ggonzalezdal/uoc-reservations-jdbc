package edu.uoc.dao;

import edu.uoc.db.Database;
import edu.uoc.model.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    /**
     * Checks whether ALL given tableIds exist and are active.
     * Transaction-aware: uses the provided Connection.
     */
    public boolean allActiveExistByIds(Connection conn, List<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) return false;

        List<Long> distinct = tableIds.stream().distinct().toList();

        String sql = """
        SELECT COUNT(DISTINCT table_id) AS cnt
        FROM tables
        WHERE active = true
          AND table_id = ANY (?::bigint[])
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setArray(1, conn.createArrayOf("bigint", distinct.toArray(new Long[0])));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long cnt = rs.getLong("cnt");
                return cnt == distinct.size();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error checking table ids " + tableIds, e);
        }
    }


    public List<Table> findAllActive(Connection conn) {
        String sql = """
        SELECT table_id, table_code, capacity, active
        FROM tables
        WHERE active = true
        ORDER BY table_code
        """;

        List<Table> tables = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tables.add(new Table(
                        rs.getLong("table_id"),
                        rs.getString("table_code"),
                        rs.getInt("capacity"),
                        rs.getBoolean("active")
                ));
            }

            return tables;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching active tables", e);
        }
    }

    public List<Table> findAllActive() {
        try (Connection conn = Database.getConnection()) {
            return findAllActive(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching active tables", e);
        }
    }

    public int sumCapacityByIds(Connection conn, List<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return 0;
        }

        List<Long> distinct = tableIds.stream().distinct().toList();

        String sql = """
        SELECT COALESCE(SUM(capacity), 0) AS total
        FROM tables
        WHERE active = true
          AND table_id = ANY (?::bigint[])
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setArray(1, conn.createArrayOf("bigint", distinct.toArray(new Long[0])));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("total");
            }

        } catch (Exception e) {
            throw new RuntimeException("Error summing capacities for table ids " + tableIds, e);
        }
    }

    public boolean existsById(long tableId) {
        String sql = "SELECT 1 FROM tables WHERE table_id = ?";

        try (
                Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setLong(1, tableId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error checking table id " + tableId, e);
        }
    }

    public boolean setActive(long tableId, boolean active) {
        String sql = "UPDATE tables SET active = ? WHERE table_id = ?";

        try (
                Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBoolean(1, active);
            ps.setLong(2, tableId);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Error updating active flag for table id " + tableId, e);
        }
    }

}

