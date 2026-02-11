package edu.uoc.model;

/**
 * Domain model representing a physical restaurant table.
 *
 * <p>This entity maps to the {@code tables} table.</p>
 *
 * <p>The object is immutable. Changes to its state (e.g., activation/deactivation)
 * are performed through {@code TableDao} operations that update the database.</p>
 *
 * @since 1.0
 */
public class Table {

    private final long id;
    private final String code;
    private final int capacity;
    private final boolean active;

    /**
     * Creates a table domain object.
     *
     * @param id       unique table identifier
     * @param code     table code (e.g., T1, T4)
     * @param capacity maximum number of guests supported
     * @param active   whether the table is currently available for reservations
     */
    public Table(long id, String code, int capacity, boolean active) {
        this.id = id;
        this.code = code;
        this.capacity = capacity;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Indicates whether the table is active.
     *
     * <p>Inactive tables cannot be assigned to reservations.</p>
     */
    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return """
                Table{
                    id=%d,
                    code='%s',
                    capacity=%d,
                    active=%s
                }
                """.formatted(
                id,
                code,
                capacity,
                active
        );
    }
}
