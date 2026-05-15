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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketsServiceImplTest {

    @Mock
    private IIssuedTicketRepository ticketRepository;

    @Mock
    private ITicketValidationRepository validationRepository;

    @InjectMocks
    private TicketsServiceImpl service;

    private IssueTicketRequest issueRequest;

    @BeforeEach
    void setUp() {
        issueRequest = new IssueTicketRequest();
        issueRequest.setOrderId(UUID.randomUUID());
        issueRequest.setEventId(UUID.randomUUID());
        issueRequest.setTicketTypeId(UUID.randomUUID());
        issueRequest.setTicketType("VIP");
        issueRequest.setHolderName("Ana Perez");
        issueRequest.setHolderEmail("ana@example.com");
        issueRequest.setHolderDocument("CC123");
        issueRequest.setPrice(new BigDecimal("150000"));
    }

    @Test
    void issueTicket_generatesUniqueQrAndPersists() {
        when(ticketRepository.existsByQrCode(anyString())).thenReturn(false);
        when(ticketRepository.save(any(IssuedTicket.class))).thenAnswer(inv -> {
            IssuedTicket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setIssuedAt(LocalDateTime.now());
            return t;
        });

        IssuedTicketResponse response = service.issueTicket(issueRequest);

        assertNotNull(response.id());
        assertNotNull(response.qrCode());
        assertEquals(TicketStatus.ISSUED, response.status());
        assertEquals("VIP", response.ticketType());
        assertEquals(issueRequest.getEventId(), response.eventId());

        ArgumentCaptor<IssuedTicket> captor = ArgumentCaptor.forClass(IssuedTicket.class);
        verify(ticketRepository).save(captor.capture());
        assertEquals(TicketStatus.ISSUED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getQrCode());
    }

    @Test
    void getTicketById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.getTicketById(id));
    }

    @Test
    void validateTicket_successWhenIssued_marksAsUsed() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.ISSUED);
        when(ticketRepository.findByQrCode("QR-1")).thenReturn(Optional.of(ticket));
        when(validationRepository.save(any(TicketValidation.class))).thenAnswer(inv -> {
            TicketValidation v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            v.setValidatedAt(LocalDateTime.now());
            return v;
        });

        ValidateTicketRequest req = new ValidateTicketRequest();
        req.setQrCode("QR-1");
        req.setGateLocation("Puerta 1");
        req.setValidatedBy("logistica-01");

        ValidationResponse response = service.validateTicket(req);

        assertEquals(ValidationResult.SUCCESS, response.result());
        assertEquals(TicketStatus.USED, ticket.getStatus());
        assertNotNull(ticket.getUsedAt());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void validateTicket_rejectsAlreadyUsed() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.USED);
        when(ticketRepository.findByQrCode("QR-2")).thenReturn(Optional.of(ticket));
        when(validationRepository.save(any(TicketValidation.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidateTicketRequest req = new ValidateTicketRequest();
        req.setQrCode("QR-2");

        ValidationResponse response = service.validateTicket(req);

        assertEquals(ValidationResult.ALREADY_USED, response.result());
        verify(ticketRepository, never()).save(any(IssuedTicket.class));
    }

    @Test
    void validateTicket_rejectsRevoked() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.REVOKED);
        when(ticketRepository.findByQrCode("QR-3")).thenReturn(Optional.of(ticket));
        when(validationRepository.save(any(TicketValidation.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidateTicketRequest req = new ValidateTicketRequest();
        req.setQrCode("QR-3");

        ValidationResponse response = service.validateTicket(req);

        assertEquals(ValidationResult.REVOKED, response.result());
        verify(ticketRepository, never()).save(any(IssuedTicket.class));
    }

    @Test
    void validateTicket_notFoundQr_logsValidation() {
        when(ticketRepository.findByQrCode("QR-X")).thenReturn(Optional.empty());
        when(validationRepository.save(any(TicketValidation.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidateTicketRequest req = new ValidateTicketRequest();
        req.setQrCode("QR-X");

        ValidationResponse response = service.validateTicket(req);

        assertEquals(ValidationResult.NOT_FOUND, response.result());
        verify(validationRepository).save(any(TicketValidation.class));
    }

    @Test
    void revokeTicket_succeedsOnIssued() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.ISSUED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(IssuedTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        IssuedTicketResponse response = service.revokeTicket(ticket.getId(), "Cancelacion del evento");

        assertEquals(TicketStatus.REVOKED, response.status());
        assertEquals("Cancelacion del evento", response.revokedReason());
        assertNotNull(response.revokedAt());
    }

    @Test
    void revokeTicket_failsIfAlreadyUsed() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.USED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
                () -> service.revokeTicket(ticket.getId(), "tarde"));
        verify(ticketRepository, never()).save(any(IssuedTicket.class));
    }

    @Test
    void revokeTicket_failsIfAlreadyRevoked() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.REVOKED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
                () -> service.revokeTicket(ticket.getId(), "duplicada"));
    }

    @Test
    void getValidationsByTicket_returnsHistory() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.existsById(ticketId)).thenReturn(true);

        TicketValidation v1 = new TicketValidation();
        v1.setId(UUID.randomUUID());
        v1.setIssuedTicketId(ticketId);
        v1.setQrCode("QR-1");
        v1.setResult(ValidationResult.SUCCESS);
        v1.setValidatedAt(LocalDateTime.now());

        when(validationRepository.findByIssuedTicketIdOrderByValidatedAtDesc(ticketId))
                .thenReturn(List.of(v1));

        List<ValidationResponse> responses = service.getValidationsByTicket(ticketId);

        assertEquals(1, responses.size());
        assertEquals(ValidationResult.SUCCESS, responses.get(0).result());
    }

    @Test
    void getValidationsByTicket_throwsIfTicketMissing() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.existsById(ticketId)).thenReturn(false);

        assertThrows(TicketNotFoundException.class, () -> service.getValidationsByTicket(ticketId));
    }

    private IssuedTicket buildIssuedTicket(TicketStatus status) {
        IssuedTicket ticket = new IssuedTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setOrderId(UUID.randomUUID());
        ticket.setEventId(UUID.randomUUID());
        ticket.setTicketTypeId(UUID.randomUUID());
        ticket.setTicketType("General");
        ticket.setHolderName("Juan");
        ticket.setHolderEmail("juan@example.com");
        ticket.setPrice(new BigDecimal("80000"));
        ticket.setQrCode("QR-" + ticket.getId());
        ticket.setStatus(status);
        ticket.setIssuedAt(LocalDateTime.now().minusHours(1));
        if (status == TicketStatus.USED) {
            ticket.setUsedAt(LocalDateTime.now().minusMinutes(10));
        }
        if (status == TicketStatus.REVOKED) {
            ticket.setRevokedAt(LocalDateTime.now().minusMinutes(20));
            ticket.setRevokedReason("test");
        }
        return ticket;
    }
}
