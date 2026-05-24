package co.empresa.vivaeventos.tickets.domain.service;

import co.empresa.vivaeventos.tickets.domain.model.Dto.IssueTicketRequest;
import co.empresa.vivaeventos.tickets.domain.model.Dto.IssuedTicketResponse;
import co.empresa.vivaeventos.tickets.domain.model.IssuedTicket;
import co.empresa.vivaeventos.tickets.domain.model.TicketStatus;
import co.empresa.vivaeventos.tickets.domain.repository.IIssuedTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketsServiceImplTest {

    @Mock
    private IIssuedTicketRepository ticketRepository;

    @InjectMocks
    private TicketsServiceImpl service;

    private IssueTicketRequest issueRequest;

    @BeforeEach
    void setUp() {
        issueRequest = new IssueTicketRequest();
        issueRequest.setOrderId(UUID.randomUUID());
        issueRequest.setEventId(UUID.randomUUID());
        issueRequest.setTicketTypeId(UUID.randomUUID());
        issueRequest.setTicketType("VIP");
        issueRequest.setHolderName("Ana Perez");
        issueRequest.setHolderEmail("ana@example.com");
        issueRequest.setHolderDocument("CC123");
        issueRequest.setPrice(new BigDecimal("150000"));
    }

    @Test
    void issueTicket_generatesUniqueQrAndPersists() {
        when(ticketRepository.existsByQrCode(anyString())).thenReturn(false);
        when(ticketRepository.save(any(IssuedTicket.class))).thenAnswer(inv -> {
            IssuedTicket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setIssuedAt(LocalDateTime.now());
            return t;
        });

        IssuedTicketResponse response = service.issueTicket(issueRequest);

        assertNotNull(response.id());
        assertNotNull(response.qrCode());
        assertEquals(TicketStatus.ISSUED, response.status());
        assertEquals("VIP", response.ticketType());
        assertEquals(issueRequest.getEventId(), response.eventId());

        ArgumentCaptor<IssuedTicket> captor = ArgumentCaptor.forClass(IssuedTicket.class);
        verify(ticketRepository).save(captor.capture());
        assertEquals(TicketStatus.ISSUED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getQrCode());
    }

    @Test
    void getTicketById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.getTicketById(id));
    }

    @Test
    void markAsUsed_succeedsOnIssued() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.ISSUED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(IssuedTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        IssuedTicketResponse response = service.markAsUsed(ticket.getId());

        assertEquals(TicketStatus.USED, response.status());
        assertNotNull(response.usedAt());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void markAsUsed_failsIfAlreadyUsed() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.USED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class, () -> service.markAsUsed(ticket.getId()));
        verify(ticketRepository, never()).save(any(IssuedTicket.class));
    }

    @Test
    void markAsUsed_failsIfRevoked() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.REVOKED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class, () -> service.markAsUsed(ticket.getId()));
        verify(ticketRepository, never()).save(any(IssuedTicket.class));
    }

    @Test
    void markAsUsed_failsIfTicketMissing() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.markAsUsed(id));
    }

    @Test
    void revokeTicket_succeedsOnIssued() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.ISSUED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(IssuedTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        IssuedTicketResponse response = service.revokeTicket(ticket.getId(), "Cancelacion del evento");

        assertEquals(TicketStatus.REVOKED, response.status());
        assertEquals("Cancelacion del evento", response.revokedReason());
        assertNotNull(response.revokedAt());
    }

    @Test
    void revokeTicket_failsIfAlreadyUsed() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.USED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
                () -> service.revokeTicket(ticket.getId(), "tarde"));
        verify(ticketRepository, never()).save(any(IssuedTicket.class));
    }

    @Test
    void revokeTicket_failsIfAlreadyRevoked() {
        IssuedTicket ticket = buildIssuedTicket(TicketStatus.REVOKED);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
                () -> service.revokeTicket(ticket.getId(), "duplicada"));
    }

    @Test
    void issueTicket_concurrentTickets_allHaveUniqueQrCodes() throws InterruptedException {
        int threadCount = 20;
        AtomicInteger savedCount = new AtomicInteger(0);
        Set<String> qrCodes = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        when(ticketRepository.existsByQrCode(anyString())).thenReturn(false);
        when(ticketRepository.save(any(IssuedTicket.class))).thenAnswer(inv -> {
            IssuedTicket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setIssuedAt(LocalDateTime.now());
            savedCount.incrementAndGet();
            qrCodes.add(t.getQrCode());
            return t;
        });

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    service.issueTicket(issueRequest);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        assertEquals(threadCount, savedCount.get(), "Todos los tickets deben haberse guardado");
        assertEquals(threadCount, qrCodes.size(), "Todos los QR deben ser unicos");
        verify(ticketRepository, times(threadCount)).save(any(IssuedTicket.class));
    }

    private IssuedTicket buildIssuedTicket(TicketStatus status) {
        IssuedTicket ticket = new IssuedTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setOrderId(UUID.randomUUID());
        ticket.setEventId(UUID.randomUUID());
        ticket.setTicketTypeId(UUID.randomUUID());
        ticket.setTicketType("General");
        ticket.setHolderName("Juan");
        ticket.setHolderEmail("juan@example.com");
        ticket.setPrice(new BigDecimal("80000"));
        ticket.setQrCode("QR-" + ticket.getId());
        ticket.setStatus(status);
        ticket.setIssuedAt(LocalDateTime.now().minusHours(1));
        if (status == TicketStatus.USED) {
            ticket.setUsedAt(LocalDateTime.now().minusMinutes(10));
        }
        if (status == TicketStatus.REVOKED) {
            ticket.setRevokedAt(LocalDateTime.now().minusMinutes(20));
            ticket.setRevokedReason("test");
        }
        return ticket;
    }
}
