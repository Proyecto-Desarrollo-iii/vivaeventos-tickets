package co.empresa.vivaeventos.tickets.domain.model.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidateTicketRequest {

    @NotBlank(message = "qrCode es obligatorio")
    private String qrCode;

    private String gateLocation;

    private String validatedBy;
}
