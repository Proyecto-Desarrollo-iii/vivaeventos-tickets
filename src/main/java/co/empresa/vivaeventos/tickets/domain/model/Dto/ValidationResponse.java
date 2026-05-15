package co.empresa.vivaeventos.tickets.domain.model.Dto;

import co.empresa.vivaeventos.tickets.domain.model.ValidationResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record ValidationResponse(
        UUID validationId,
        UUID issuedTicketId,
        UUID eventId,
        String eventName,
        String ticketType,
        String holderName,
        String holderEmail,
        String qrCode,
        ValidationResult result,
        String message,
        String gateLocation,
        String validatedBy,
        LocalDateTime validatedAt
) {}
