package co.empresa.vivaeventos.tickets.delivery.rest;

import co.empresa.vivaeventos.tickets.domain.model.Dto.IssueTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.IssuedTicketResponse;
import co.empresa.vivaeventos.tickets.domain.model.Dto.RevokeTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.ValidateTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.ValidationResponse;
import co.empresa.vivaeventos.tickets.domain.model.ValidationResult;
import co.empresa.vivaeventos.tickets.domain.service.ITicketsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issued-tickets")
public class TicketsController {

    private final ITicketsService ticketsService;

    public TicketsController(ITicketsService ticketsService) {
        this.ticketsService = ticketsService;
    }

    @PostMapping("/issue")
    public ResponseEntity<Map<String, Object>> issueTicket(@Valid @RequestBody IssueTicketRequest request) {
        IssuedTicketResponse ticket = ticketsService.issueTicket(request);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Boleta emitida exitosamente");
        response.put("boleta", ticket);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<Map<String, Object>> getTicket(@PathVariable UUID ticketId) {
        IssuedTicketResponse ticket = ticketsService.getTicketById(ticketId);

        Map<String, Object> response = new HashMap<>();
        response.put("boleta", ticket);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr/{qrCode}")
    public ResponseEntity<Map<String, Object>> getTicketByQr(@PathVariable String qrCode) {
        IssuedTicketResponse ticket = ticketsService.getTicketByQrCode(qrCode);

        Map<String, Object> response = new HashMap<>();
        response.put("boleta", ticket);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<Map<String, Object>> getTicketsByEvent(@PathVariable UUID eventId) {
        List<IssuedTicketResponse> tickets = ticketsService.getTicketsByEvent(eventId);

        Map<String, Object> response = new HashMap<>();
        response.put("boletas", tickets);
        response.put("total", tickets.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getTicketsByOrder(@PathVariable UUID orderId) {
        List<IssuedTicketResponse> tickets = ticketsService.getTicketsByOrder(orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("boletas", tickets);
        response.put("total", tickets.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateTicket(@Valid @RequestBody ValidateTicketRequest request) {
        ValidationResponse validation = ticketsService.validateTicket(request);

        Map<String, Object> response = new HashMap<>();
        response.put("validacion", validation);
        response.put("autorizado", validation.result() == ValidationResult.SUCCESS);

        HttpStatus status = validation.result() == ValidationResult.SUCCESS
                ? HttpStatus.OK
                : HttpStatus.CONFLICT;

        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/{ticketId}/revoke")
    public ResponseEntity<Map<String, Object>> revokeTicket(@PathVariable UUID ticketId,
                                                            @Valid @RequestBody RevokeTicketRequest request) {
        IssuedTicketResponse ticket = ticketsService.revokeTicket(ticketId, request.getReason());

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Boleta revocada exitosamente");
        response.put("boleta", ticket);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticketId}/validations")
    public ResponseEntity<Map<String, Object>> getValidations(@PathVariable UUID ticketId) {
        List<ValidationResponse> validations = ticketsService.getValidationsByTicket(ticketId);

        Map<String, Object> response = new HashMap<>();
        response.put("validaciones", validations);
        response.put("total", validations.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/release-by-order/{orderId}")
    public ResponseEntity<Map<String, Object>> releaseTicketsByOrder(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "Payment failed or timeout");
        ticketsService.releaseTicketsByOrder(orderId, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Boletas liberadas exitosamente");
        response.put("orderId", orderId);

        return ResponseEntity.ok(response);
    }
}
