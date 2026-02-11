package edu.uoc.model;

import java.time.OffsetDateTime;

/**
 * Domain model representing a customer.
 *
 * <p>This entity maps to the {@code customers} table.</p>
 *
 * <p>The object is immutable once created. Persistence operations
 * (insert, lookup) are handled by {@code CustomerDao}.</p>
 *
 * @since 1.0
 */
public class Customer {

    private final long id;
    private final String fullName;
    private final String phone;
    private final String email;              // nullable
    private final OffsetDateTime createdAt;

    /**
     * Creates a customer domain object.
     *
     * @param id         unique customer identifier
     * @param fullName   customer's full name
     * @param phone      customer's phone number
     * @param email      customer's email (nullable)
     * @param createdAt  timestamp when customer was created
     */
    public Customer(long id,
                    String fullName,
                    String phone,
                    String email,
                    OffsetDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return """
                Customer{
                    id=%d,
                    fullName='%s',
                    phone='%s',
                    email='%s',
                    createdAt=%s
                }
                """.formatted(
                id,
                fullName,
                phone,
                email,
                createdAt
        );
    }
}
