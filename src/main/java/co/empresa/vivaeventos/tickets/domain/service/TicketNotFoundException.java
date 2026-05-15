package co.empresa.vivaeventos.tickets.domain.service;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String message) {
        super(message);
    }
}
