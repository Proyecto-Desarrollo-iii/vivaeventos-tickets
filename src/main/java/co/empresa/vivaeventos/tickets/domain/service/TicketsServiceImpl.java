package co.empresa.vivaeventos.tickets.domain.service;

import co.empresa.vivaeventos.tickets.domain.model.Dto.IssueTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.IssuedTicketResponse;
import co.empresa.vivaeventos.tickets.domain.model.IssuedTicket;
import co.empresa.vivaeventos.tickets.domain.model.TicketStatus;
import co.empresa.vivaeventos.tickets.domain.repository.IIssuedTicketRepository;
import co.empresa.vivaeventos.tickets.domain.util.QRCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketsServiceImpl implements ITicketsService {

    private final IIssuedTicketRepository ticketRepository;

    public TicketsServiceImpl(IIssuedTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
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
    public IssuedTicketResponse markAsUsed(UUID ticketId) {
        IssuedTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Boleta no encontrada: " + ticketId));

        if (ticket.getStatus() == TicketStatus.REVOKED) {
            throw new IllegalStateException("No se puede marcar como usada una boleta revocada");
        }
        if (ticket.getStatus() == TicketStatus.USED) {
            throw new IllegalStateException("La boleta ya fue utilizada");
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());
        return mapToResponse(ticketRepository.save(ticket));
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
        String qrCode = ticket.getQrCode();
        String qrImage = QRCodeGenerator.toBase64DataUri(qrCode);

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
                qrCode,
                qrImage,
                ticket.getStatus(),
                ticket.getIssuedAt(),
                ticket.getUsedAt(),
                ticket.getRevokedAt(),
                ticket.getRevokedReason()
        );
    }
}
