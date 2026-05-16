package co.empresa.vivaeventos.tickets.domain.service;

import co.empresa.vivaeventos.tickets.domain.model.Dto.IssueTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.IssuedTicketResponse;
import co.empresa.vivaeventos.tickets.domain.model.Dto.ValidateTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.ValidationResponse;

import java.util.List;
import java.util.UUID;

public interface ITicketsService {

    IssuedTicketResponse issueTicket(IssueTicketRequest request);

    IssuedTicketResponse getTicketById(UUID ticketId);

    IssuedTicketResponse getTicketByQrCode(String qrCode);

    List<IssuedTicketResponse> getTicketsByEvent(UUID eventId);

    List<IssuedTicketResponse> getTicketsByOrder(UUID orderId);

    ValidationResponse validateTicket(ValidateTicketRequest request);

    IssuedTicketResponse revokeTicket(UUID ticketId, String reason);

    List<ValidationResponse> getValidationsByTicket(UUID ticketId);

    void releaseTicketsByOrder(UUID orderId, String reason);
}
