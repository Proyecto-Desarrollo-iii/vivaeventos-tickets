package co.empresa.vivaeventos.tickets.domain.model.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevokeTicketRequest {

    @NotBlank(message = "reason es obligatorio")
    private String reason;
}
