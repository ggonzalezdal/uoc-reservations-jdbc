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

/**
 * Data Access Object for {@link Table}.
 *
 * <p>Responsible for persistence operations related to the {@code tables} table.</p>
 *
 * <p>This DAO provides:</p>
 * <ul>
 *   <li>Lookup by ID and by code</li>
 *   <li>List all tables (and active-only)</li>
 *   <li>Validation helpers used by the service layer</li>
 *   <li>Capacity aggregation helpers for table assignment rules</li>
 * </ul>
 *
 * <p>Transaction-aware methods accept a {@link Connection} and do not manage
 * commit/rollback. Convenience overloads open their own connection.</p>
 *
 * @since 1.0
 */
public class TableDao {

    /**
     * Retrieves all tables ordered by table code.
     *
     * @return list of all tables (empty list if none exist)
     * @throws RuntimeException if a database error occurs
     */
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
                tables.add(mapRow(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching tables", e);
        }

        return tables;
    }

    /**
     * Retrieves a table by its identifier.
     *
     * @param id table ID
     * @return optional table if found, otherwise empty
     * @throws RuntimeException if a database error occurs
     */
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
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching table with id " + id, e);
        }
    }

    /**
     * Retrieves a table by its code (e.g., "T1", "T4").
     *
     * @param code table code
     * @return optional table if found, otherwise empty
     * @throws RuntimeException if a database error occurs
     */
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
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching table with code " + code, e);
        }
    }

    /**
     * Checks whether all provided table IDs exist and are active.
     *
     * <p>This method is typically used by the service layer to validate table assignments.</p>
     *
     * <p>Transaction-aware: uses the provided connection; does not commit/rollback.</p>
     *
     * @param conn     existing database connection
     * @param tableIds table IDs to validate
     * @return true if all IDs exist and are active; false otherwise (including empty input)
     * @throws RuntimeException if a database error occurs
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

    /**
     * Retrieves all active tables using the provided connection.
     *
     * <p>Transaction-aware: uses the provided connection.</p>
     *
     * @param conn existing database connection
     * @return list of active tables (empty if none)
     * @throws RuntimeException if a database error occurs
     */
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
                tables.add(mapRow(rs));
            }

            return tables;

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching active tables", e);
        }
    }

    /**
     * Convenience overload for retrieving active tables without an explicit transaction.
     *
     * @return list of active tables
     * @throws RuntimeException if a database error occurs
     */
    public List<Table> findAllActive() {
        try (Connection conn = Database.getConnection()) {
            return findAllActive(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching active tables", e);
        }
    }

    /**
     * Sums the capacity of the specified active tables.
     *
     * <p>Only active tables contribute to the sum.</p>
     *
     * <p>Transaction-aware: uses the provided connection.</p>
     *
     * @param conn     existing database connection
     * @param tableIds table IDs (duplicates are ignored)
     * @return total capacity (0 if input is empty or none active)
     * @throws RuntimeException if a database error occurs
     */
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

    /**
     * Checks whether a table exists by ID.
     *
     * <p>Non-transactional: opens its own connection.</p>
     *
     * @param tableId table ID
     * @return true if exists, false otherwise
     * @throws RuntimeException if a database error occurs
     */
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

    /**
     * Updates the active flag of a table.
     *
     * <p>Non-transactional: opens its own connection.</p>
     *
     * @param tableId table ID
     * @param active  new active value
     * @return true if exactly one row was updated
     * @throws RuntimeException if a database error occurs
     */
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

    // -------------------------------------------------------------------------
    // Internal mapping helper
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link ResultSet} row into a {@link Table} domain object.
     *
     * @param rs result set positioned at a valid row
     * @return mapped table
     * @throws SQLException if column access fails
     */
    private Table mapRow(ResultSet rs) throws SQLException {
        return new Table(
                rs.getLong("table_id"),
                rs.getString("table_code"),
                rs.getInt("capacity"),
                rs.getBoolean("active")
        );
    }
}
