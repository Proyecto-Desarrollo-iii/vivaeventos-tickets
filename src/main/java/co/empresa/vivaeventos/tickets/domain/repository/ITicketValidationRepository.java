package co.empresa.vivaeventos.tickets.domain.repository;

import co.empresa.vivaeventos.tickets.domain.model.TicketValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ITicketValidationRepository extends JpaRepository<TicketValidation, UUID> {

    List<TicketValidation> findByIssuedTicketIdOrderByValidatedAtDesc(UUID issuedTicketId);

    List<TicketValidation> findByEventIdOrderByValidatedAtDesc(UUID eventId);
}
