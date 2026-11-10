package mk.ukim.finki.testing.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BookingTransactionIT {

    @Autowired BookingService service;
    @Autowired RegistrationRepository registrations;

    @MockitoBean MembershipGateway membership;

    @BeforeEach
    void setUp() {
        registrations.deleteAll();
        when(membership.isEligible(anyString())).thenReturn(true);
    }

    @AfterEach
    void cleanUp() {
        registrations.deleteAll();
    }

    private BookingRequest request(String reference, String workshopCode, String email, String memberId) {
        return new BookingRequest(reference, workshopCode, "Ana", email, memberId, 1);
    }

    //Successful scenario
    @Test
    void saveWholeBatch() {
        service.registerBatch(List.of(request("REF-1", "JAVA-01", "ana@example.com", "C-01"), request("REF-2", "JAVA-01", "boris@example.com", "C-01"), request("REF-3", "JAVA-01", "vesna@example.com", "C-01")));

        assertEquals(3, registrations.count());
    }

    //Unsuccessful scenario - last request is for a closed workshop
    @Test
    void rollbackBatchWhenWorkshopIsClosed() {
        BookingFailure failure = assertThrows(BookingFailure.class, () -> service.registerBatch(List.of(request("REF-1", "JAVA-01", "ana@example.com", "C-01"), request("REF-2", "JAVA-01", "boris@example.com", "C-01"), request("REF-3", "CLOSED-01", "vesna@example.com", "C-01"))));

        assertEquals("WORKSHOP_CLOSED", failure.getCode());
        assertEquals(0, registrations.count());
    }

    //Unsuccessful scenario - one member is not eligible
    @Test
    void rollbackBatchWhenMemberIsIneligible() {
        when(membership.isEligible("C-02")).thenReturn(false);

        BookingFailure failure = assertThrows(BookingFailure.class, () -> service.registerBatch(List.of(request("REF-1", "JAVA-01", "ana@example.com", "C-01"), request("REF-2", "JAVA-01", "boris@example.com", "C-02"))));

        assertEquals("MEMBER_INELIGIBLE", failure.getCode());
        assertEquals(0, registrations.count());
    }

    //Unsuccessful scenario - duplicate reference inside the batch
    @Test
    void rollbackBatchWhenReferenceIsDuplicate() {
        assertThrows(DataIntegrityViolationException.class, () -> service.registerBatch(List.of(request("REF-1", "JAVA-01", "ana@example.com", "C-01"), request("REF-1", "JAVA-01", "boris@example.com", "C-01"))));

        assertEquals(0, registrations.count());
    }

    //Unsuccessful scenario - batch with more than 10 requests
    @Test
    void rejectTooLargeBatch() {
        List<BookingRequest> requests = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            requests.add(request("REF-" + i, "JAVA-01", "user" + i + "@example.com", "C-01"));
        }

        BookingFailure failure = assertThrows(BookingFailure.class, () -> service.registerBatch(requests));

        assertEquals("INVALID_INPUT", failure.getCode());
        assertEquals(0, registrations.count());
    }
}