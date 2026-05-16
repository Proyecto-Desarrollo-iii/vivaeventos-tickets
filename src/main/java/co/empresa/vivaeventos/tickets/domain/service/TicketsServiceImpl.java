package co.empresa.vivaeventos.tickets.domain.service;

import co.empresa.vivaeventos.tickets.domain.model.Dto.IssueTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.IssuedTicketResponse;
import co.empresa.vivaeventos.tickets.domain.model.Dto.ValidateTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.ValidationResponse;
import co.empresa.vivaeventos.tickets.domain.model.IssuedTicket;
import co.empresa.vivaeventos.tickets.domain.model.TicketStatus;
import co.empresa.vivaeventos.tickets.domain.model.TicketValidation;
import co.empresa.vivaeventos.tickets.domain.model.ValidationResult;
import co.empresa.vivaeventos.tickets.domain.repository.IIssuedTicketRepository;
import co.empresa.vivaeventos.tickets.domain.repository.ITicketValidationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketsServiceImpl implements ITicketsService {

    private final IIssuedTicketRepository ticketRepository;
    private final ITicketValidationRepository validationRepository;

    public TicketsServiceImpl(IIssuedTicketRepository ticketRepository,
                              ITicketValidationRepository validationRepository) {
        this.ticketRepository = ticketRepository;
        this.validationRepository = validationRepository;
    }

    @Override
    @Transactional
    public IssuedTicketResponse issueTicket(IssueTicketRequest request) {
        IssuedTicket ticket = new IssuedTicket();
        ticket.setOrderId(request.getOrderId());
        ticket.setEventId(request.getEventId());
        ticket.setTicketTypeId(request.getTicketTypeId());
        ticket.setTicketType(request.getTicketType());
        ticket.setEventName(request.getEventName());
        ticket.setHolderName(request.getHolderName());
        ticket.setHolderEmail(request.getHolderEmail());
        ticket.setHolderDocument(request.getHolderDocument());
        ticket.setPrice(request.getPrice());
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setQrCode(generateUniqueQrCode());

        IssuedTicket saved = ticketRepository.save(ticket);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public IssuedTicketResponse getTicketById(UUID ticketId) {
        IssuedTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Boleta no encontrada: " + ticketId));
        return mapToResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public IssuedTicketResponse getTicketByQrCode(String qrCode) {
        IssuedTicket ticket = ticketRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new TicketNotFoundException("Boleta no encontrada para el QR proporcionado"));
        return mapToResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssuedTicketResponse> getTicketsByEvent(UUID eventId) {
        return ticketRepository.findByEventId(eventId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssuedTicketResponse> getTicketsByOrder(UUID orderId) {
        return ticketRepository.findByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ValidationResponse validateTicket(ValidateTicketRequest request) {
        Optional<IssuedTicket> opt = ticketRepository.findByQrCode(request.getQrCode());

        TicketValidation validation = new TicketValidation();
        validation.setQrCode(request.getQrCode());
        validation.setGateLocation(request.getGateLocation());
        validation.setValidatedBy(request.getValidatedBy());

        if (opt.isEmpty()) {
            validation.setResult(ValidationResult.NOT_FOUND);
            TicketValidation saved = validationRepository.save(validation);
            return buildValidationResponse(saved, "QR no corresponde a una boleta emitida");
        }

        IssuedTicket ticket = opt.get();
        validation.setIssuedTicketId(ticket.getId());
        validation.setEventId(ticket.getEventId());

        switch (ticket.getStatus()) {
            case REVOKED -> {
                validation.setResult(ValidationResult.REVOKED);
                TicketValidation saved = validationRepository.save(validation);
                return buildValidationResponse(saved, "La boleta fue revocada", ticket);
            }
            case USED -> {
                validation.setResult(ValidationResult.ALREADY_USED);
                TicketValidation saved = validationRepository.save(validation);
                return buildValidationResponse(saved, "La boleta ya fue utilizada", ticket);
            }
            case ISSUED -> {
                ticket.setStatus(TicketStatus.USED);
                ticket.setUsedAt(LocalDateTime.now());
                ticketRepository.save(ticket);

                validation.setResult(ValidationResult.SUCCESS);
                TicketValidation saved = validationRepository.save(validation);
                return buildValidationResponse(saved, "Ingreso autorizado", ticket);
            }
            default -> {
                validation.setResult(ValidationResult.NOT_FOUND);
                TicketValidation saved = validationRepository.save(validation);
                return buildValidationResponse(saved, "Estado de boleta no soportado", ticket);
            }
        }
    }

    @Override
    @Transactional
    public IssuedTicketResponse revokeTicket(UUID ticketId, String reason) {
        IssuedTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Boleta no encontrada: " + ticketId));

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new IllegalStateException("No se puede revocar una boleta que ya fue utilizada");
        }
        if (ticket.getStatus() == TicketStatus.REVOKED) {
            throw new IllegalStateException("La boleta ya estaba revocada");
        }

        ticket.setStatus(TicketStatus.REVOKED);
        ticket.setRevokedAt(LocalDateTime.now());
        ticket.setRevokedReason(reason);

        return mapToResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationResponse> getValidationsByTicket(UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException("Boleta no encontrada: " + ticketId);
        }
        return validationRepository.findByIssuedTicketIdOrderByValidatedAtDesc(ticketId).stream()
                .map(v -> buildValidationResponse(v, null))
                .toList();
    }

    @Override
    @Transactional
    public void releaseTicketsByOrder(UUID orderId, String reason) {
        List<IssuedTicket> tickets = ticketRepository.findByOrderId(orderId);
        for (IssuedTicket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.ISSUED) {
                ticket.setStatus(TicketStatus.REVOKED);
                ticket.setRevokedAt(LocalDateTime.now());
                ticket.setRevokedReason(reason);
                ticketRepository.save(ticket);
            }
        }
    }

    private String generateUniqueQrCode() {
        String candidate;
        int attempts = 0;
        do {
            candidate = UUID.randomUUID().toString().replace("-", "")
                    + Long.toHexString(System.nanoTime());
            attempts++;
            if (attempts > 5) {
                throw new IllegalStateException("No fue posible generar un QR unico");
            }
        } while (ticketRepository.existsByQrCode(candidate));
        return candidate;
    }

    private IssuedTicketResponse mapToResponse(IssuedTicket ticket) {
        return new IssuedTicketResponse(
                ticket.getId(),
                ticket.getOrderId(),
                ticket.getEventId(),
                ticket.getEventName(),
                ticket.getTicketTypeId(),
                ticket.getTicketType(),
                ticket.getHolderName(),
                ticket.getHolderEmail(),
                ticket.getHolderDocument(),
                ticket.getPrice(),
                ticket.getQrCode(),
                ticket.getStatus(),
                ticket.getIssuedAt(),
                ticket.getUsedAt(),
                ticket.getRevokedAt(),
                ticket.getRevokedReason()
        );
    }

    private ValidationResponse buildValidationResponse(TicketValidation validation, String message) {
        return buildValidationResponse(validation, message, null);
    }

    private ValidationResponse buildValidationResponse(TicketValidation validation, String message, IssuedTicket ticket) {
        return new ValidationResponse(
                validation.getId(),
                validation.getIssuedTicketId(),
                validation.getEventId(),
                ticket != null ? ticket.getEventName() : null,
                ticket != null ? ticket.getTicketType() : null,
                ticket != null ? ticket.getHolderName() : null,
                ticket != null ? ticket.getHolderEmail() : null,
                validation.getQrCode(),
                validation.getResult(),
                message,
                validation.getGateLocation(),
                validation.getValidatedBy(),
                validation.getValidatedAt()
        );
    }
}
