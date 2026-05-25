package co.empresa.vivaeventos.tickets.domain.repository;

import co.empresa.vivaeventos.tickets.domain.model.IssuedTicket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM IssuedTicket t WHERE t.id = :id")
    Optional<IssuedTicket> findByIdWithLock(UUID id);
}
