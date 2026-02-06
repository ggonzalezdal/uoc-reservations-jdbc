package edu.uoc.model;

public class Table {
    private final long id;
    private final String code;
    private final int capacity;
    private final boolean active;

    public Table(long id, String code, int capacity, boolean active) {
        this.id = id;
        this.code = code;
        this.capacity = capacity;
        this.active = active;
    }

    public long getId() { return id; }
    public String getCode() { return code; }
    public int getCapacity() { return capacity; }
    public boolean isActive() { return active; }

    @Override
    public String toString() {
        return "Table{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", capacity=" + capacity +
                ", active=" + active +
                '}';
    }
}

