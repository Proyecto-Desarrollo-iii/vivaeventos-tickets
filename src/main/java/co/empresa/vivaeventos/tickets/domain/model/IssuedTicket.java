package co.empresa.vivaeventos.tickets.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "issued_tickets", indexes = {
        @Index(name = "idx_issued_tickets_qr", columnList = "qr_code", unique = true),
        @Index(name = "idx_issued_tickets_event", columnList = "event_id"),
        @Index(name = "idx_issued_tickets_order", columnList = "order_id")
})
@Getter
@Setter
public class IssuedTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(name = "ticket_type", nullable = false)
    private String ticketType;

    @Column(name = "event_name")
    private String eventName;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Column(name = "holder_email", nullable = false)
    private String holderEmail;

    @Column(name = "holder_document")
    private String holderDocument;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "qr_code", nullable = false, unique = true, length = 128)
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TicketStatus status;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TicketStatus.ISSUED;
        }
    }
}
