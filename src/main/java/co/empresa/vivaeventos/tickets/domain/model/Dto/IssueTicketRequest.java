package co.empresa.vivaeventos.tickets.domain.model.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class IssueTicketRequest {

    @NotNull(message = "orderId es obligatorio")
    private UUID orderId;

    @NotNull(message = "eventId es obligatorio")
    private UUID eventId;

    @NotNull(message = "ticketTypeId es obligatorio")
    private UUID ticketTypeId;

    @NotBlank(message = "ticketType es obligatorio")
    private String ticketType;

    private String eventName;

    @NotBlank(message = "holderName es obligatorio")
    private String holderName;

    @NotBlank(message = "holderEmail es obligatorio")
    @Email(message = "holderEmail debe ser un correo valido")
    private String holderEmail;

    private String holderDocument;

    @NotNull(message = "price es obligatorio")
    @PositiveOrZero(message = "price no puede ser negativo")
    private BigDecimal price;

    private UUID userId;
}
