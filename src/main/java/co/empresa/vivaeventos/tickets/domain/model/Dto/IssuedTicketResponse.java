package co.empresa.vivaeventos.tickets.domain.model.Dto;

import co.empresa.vivaeventos.tickets.domain.model.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record IssuedTicketResponse(
        UUID id,
        UUID orderId,
        UUID eventId,
        String eventName,
        UUID ticketTypeId,
        String ticketType,
        String holderName,
        String holderEmail,
        String holderDocument,
        BigDecimal price,
        String qrCode,
        TicketStatus status,
        LocalDateTime issuedAt,
        LocalDateTime usedAt,
        LocalDateTime revokedAt,
        String revokedReason
) {}
