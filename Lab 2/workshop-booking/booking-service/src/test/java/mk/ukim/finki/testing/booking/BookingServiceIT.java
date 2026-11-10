package mk.ukim.finki.testing.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@DataJpaTest
@Import({BookingService.class, LocalValidatorFactoryBean.class})
class BookingServiceIT {

    @Autowired BookingService service;
    @Autowired RegistrationRepository registrations;
    @Autowired WorkshopRepository workshops;
    @Autowired TestEntityManager em;

    @MockitoBean MembershipGateway membership;

    @BeforeEach
    void setUp() {
        workshops.saveAndFlush(new Workshop("JAVA-01", "Java Workshop", true));
        workshops.saveAndFlush(new Workshop("CLOSED-01", "Closed Workshop", false));
        when(membership.isEligible("C-01")).thenReturn(true);
    }

    //Successful scenario
    @Test
    void registerAndSaveNormalizedData() {
        service.register(new BookingRequest("REF-1", "JAVA-01", "  Ana  ", "ANA@Example.com", "C-01", 2));
        em.flush();
        em.clear();

        Registration loaded = registrations.findByReference("REF-1").orElseThrow();
        assertEquals("Ana", loaded.getAttendee());
        assertEquals("ana@example.com", loaded.getEmail());
        assertEquals(2, loaded.getTickets());
        assertEquals("JAVA-01", loaded.getWorkshop().getCode());
    }

    //Successful scenario
    @Test
    void findSavedRegistration() {
        service.register(new BookingRequest("REF-1", "JAVA-01", "Ana", "ana@example.com", "C-01", 1));
        em.flush();
        em.clear();

        RegistrationView view = service.find("REF-1");
        assertEquals("Ana", view.attendee());
        assertEquals("JAVA-01", view.workshopCode());
    }

    //Unsuccessful scenario - invalid input
    @Test
    void rejectInvalidInput() {
        BookingFailure failure = assertThrows(BookingFailure.class, () -> service.register(new BookingRequest("REF-1", "JAVA-01", "Ana", "ana@example.com", "C-01", 0)));

        assertEquals("INVALID_INPUT", failure.getCode());
        assertEquals(0, registrations.count());
        verify(membership, never()).isEligible(anyString());
    }

    //Unsuccessful scenario - workshop does not exist
    @Test
    void rejectUnknownWorkshop() {
        BookingFailure failure = assertThrows(BookingFailure.class, () -> service.register(new BookingRequest("REF-1", "NONE-01", "Ana", "ana@example.com", "C-01", 1)));

        assertEquals("WORKSHOP_NOT_FOUND", failure.getCode());
        assertEquals(0, registrations.count());
    }

    //Unsuccessful scenario - workshop is closed
    @Test
    void rejectClosedWorkshop() {
        BookingFailure failure = assertThrows(BookingFailure.class, () -> service.register(new BookingRequest("REF-1", "CLOSED-01", "Ana", "ana@example.com", "C-01", 1)));

        assertEquals("WORKSHOP_CLOSED", failure.getCode());
        assertEquals(0, registrations.count());
    }

    //Unsuccessful scenario - member is not eligible
    @Test
    void rejectIneligibleMember() {
        when(membership.isEligible("C-02")).thenReturn(false);

        BookingFailure failure = assertThrows(BookingFailure.class, () -> service.register(new BookingRequest("REF-1", "JAVA-01", "Ana", "ana@example.com", "C-02", 1)));

        assertEquals("MEMBER_INELIGIBLE", failure.getCode());
        assertEquals(0, registrations.count());
    }
}