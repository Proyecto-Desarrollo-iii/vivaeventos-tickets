package co.empresa.vivaeventos.tickets.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_validations", indexes = {
        @Index(name = "idx_ticket_validations_ticket", columnList = "issued_ticket_id"),
        @Index(name = "idx_ticket_validations_event", columnList = "event_id")
})
@Getter
@Setter
public class TicketValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issued_ticket_id")
    private UUID issuedTicketId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "qr_code", nullable = false, length = 128)
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 16)
    private ValidationResult result;

    @Column(name = "gate_location")
    private String gateLocation;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at", nullable = false, updatable = false)
    private LocalDateTime validatedAt;

    @PrePersist
    protected void onCreate() {
        if (validatedAt == null) {
            validatedAt = LocalDateTime.now();
        }
    }
}
