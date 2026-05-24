package co.empresa.vivaeventos.tickets.delivery.rest;

import co.empresa.vivaeventos.tickets.domain.model.Dto.IssueTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.IssuedTicketResponse;
import co.empresa.vivaeventos.tickets.domain.model.Dto.RevokeTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.TicketStatus;
import co.empresa.vivaeventos.tickets.domain.service.ITicketsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketsControllerTest {

    @Mock
    private ITicketsService ticketsService;

    @InjectMocks
    private TicketsController controller;

    @Test
    void issueTicket_returnsCreated() {
        IssueTicketRequest request = new IssueTicketRequest();
        request.setOrderId(UUID.randomUUID());
        request.setEventId(UUID.randomUUID());
        request.setTicketTypeId(UUID.randomUUID());
        request.setTicketType("VIP");
        request.setHolderName("Ana");
        request.setHolderEmail("ana@example.com");
        request.setPrice(new BigDecimal("100000"));

        IssuedTicketResponse expected = sampleTicketResponse(TicketStatus.ISSUED);
        when(ticketsService.issueTicket(any(IssueTicketRequest.class))).thenReturn(expected);

        ResponseEntity<Map<String, Object>> response = controller.issueTicket(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Boleta emitida exitosamente", response.getBody().get("mensaje"));
        assertEquals(expected, response.getBody().get("boleta"));
    }

    @Test
    void getTicket_returnsOk() {
        UUID id = UUID.randomUUID();
        IssuedTicketResponse expected = sampleTicketResponse(TicketStatus.ISSUED);
        when(ticketsService.getTicketById(id)).thenReturn(expected);

        ResponseEntity<Map<String, Object>> response = controller.getTicket(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody().get("boleta"));
    }

    @Test
    void getTicketsByEvent_returnsListAndTotal() {
        UUID eventId = UUID.randomUUID();
        IssuedTicketResponse t1 = sampleTicketResponse(TicketStatus.ISSUED);
        IssuedTicketResponse t2 = sampleTicketResponse(TicketStatus.USED);
        when(ticketsService.getTicketsByEvent(eventId)).thenReturn(List.of(t1, t2));

        ResponseEntity<Map<String, Object>> response = controller.getTicketsByEvent(eventId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().get("total"));
    }

    @Test
    void markAsUsed_returnsOk() {
        UUID id = UUID.randomUUID();
        IssuedTicketResponse expected = sampleTicketResponse(TicketStatus.USED);
        when(ticketsService.markAsUsed(id)).thenReturn(expected);

        ResponseEntity<Map<String, Object>> response = controller.markAsUsed(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Boleta marcada como utilizada", response.getBody().get("mensaje"));
        assertEquals(expected, response.getBody().get("boleta"));
    }

    @Test
    void revokeTicket_returnsOk() {
        UUID id = UUID.randomUUID();
        RevokeTicketRequest req = new RevokeTicketRequest();
        req.setReason("Cancelacion");
        IssuedTicketResponse expected = sampleTicketResponse(TicketStatus.REVOKED);
        when(ticketsService.revokeTicket(id, "Cancelacion")).thenReturn(expected);

        ResponseEntity<Map<String, Object>> response = controller.revokeTicket(id, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Boleta revocada exitosamente", response.getBody().get("mensaje"));
    }

    private IssuedTicketResponse sampleTicketResponse(TicketStatus status) {
        return new IssuedTicketResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Festival Demo",
                UUID.randomUUID(),
                "VIP",
                "Ana",
                "ana@example.com",
                "CC123",
                new BigDecimal("100000"),
                "QR-" + UUID.randomUUID(),
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                status,
                LocalDateTime.now(),
                status == TicketStatus.USED ? LocalDateTime.now() : null,
                status == TicketStatus.REVOKED ? LocalDateTime.now() : null,
                status == TicketStatus.REVOKED ? "test" : null
        );
    }
}
