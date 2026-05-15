package co.empresa.vivaeventos.tickets.domain.repository;

import co.empresa.vivaeventos.tickets.domain.model.IssuedTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IIssuedTicketRepository extends JpaRepository<IssuedTicket, UUID> {

    Optional<IssuedTicket> findByQrCode(String qrCode);

    List<IssuedTicket> findByEventId(UUID eventId);

    List<IssuedTicket> findByOrderId(UUID orderId);

    boolean existsByQrCode(String qrCode);
}
