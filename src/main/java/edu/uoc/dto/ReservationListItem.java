package edu.uoc.dto;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object representing a reservation list view.
 *
 * <p>This DTO is used when listing reservations joined with customer data.
 * It is intentionally separate from the {@code Reservation} domain model
 * to avoid coupling presentation queries with transactional entities.</p>
 *
 * <p>This class is immutable.</p>
 *
 * @since 1.0
 */
public class ReservationListItem {

    private final long reservationId;
    private final long customerId;
    private final String customerName;
    private final OffsetDateTime startAt;
    private final OffsetDateTime endAt;      // nullable
    private final int partySize;
    private final String status;
    private final String notes;              // nullable
    private final OffsetDateTime createdAt;

    /**
     * Creates a reservation list projection.
     *
     * @param reservationId unique reservation identifier
     * @param customerId    customer identifier
     * @param customerName  customer's full name
     * @param startAt       reservation start date-time
     * @param endAt         reservation end date-time (nullable)
     * @param partySize     number of guests
     * @param status        reservation status (e.g., PENDING, CONFIRMED)
     * @param notes         optional notes (nullable)
     * @param createdAt     timestamp when reservation was created
     */
    public ReservationListItem(long reservationId,
                               long customerId,
                               String customerName,
                               OffsetDateTime startAt,
                               OffsetDateTime endAt,
                               int partySize,
                               String status,
                               String notes,
                               OffsetDateTime createdAt) {
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.startAt = startAt;
        this.endAt = endAt;
        this.partySize = partySize;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public long getReservationId() { return reservationId; }
    public long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public OffsetDateTime getStartAt() { return startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public int getPartySize() { return partySize; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return """
                ReservationListItem{
                    reservationId=%d,
                    customerId=%d,
                    customerName='%s',
                    startAt=%s,
                    endAt=%s,
                    partySize=%d,
                    status='%s',
                    notes='%s',
                    createdAt=%s
                }
                """.formatted(
                reservationId,
                customerId,
                customerName,
                startAt,
                endAt,
                partySize,
                status,
                notes,
                createdAt
        );
    }
}
