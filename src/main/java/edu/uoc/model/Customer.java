package edu.uoc.model;

import java.time.OffsetDateTime;

public class Customer {
    private final long id;
    private final String fullName;
    private final String phone;
    private final String email;
    private final OffsetDateTime createdAt;

    public Customer(long id, String fullName, String phone, String email, OffsetDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}


