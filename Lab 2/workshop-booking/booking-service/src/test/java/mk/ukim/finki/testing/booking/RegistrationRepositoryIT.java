package mk.ukim.finki.testing.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class RegistrationRepositoryIT {

    @Autowired RegistrationRepository registrations;
    @Autowired WorkshopRepository workshops;
    @Autowired TestEntityManager em;

    Workshop java;

    @BeforeEach
    void setUp() {
        java = workshops.saveAndFlush(new Workshop("JAVA-01", "Java Workshop", true));
    }

    //Successful scenario
    @Test
    void saveAndLoadRegistration() {
        Registration saved = registrations.saveAndFlush(
                new Registration("REF-1", "Ana", "ana@example.com", "C-01", 2, java));
        em.clear();

        Registration loaded = registrations.findByReference("REF-1").orElseThrow();
        assertEquals(saved.getId(), loaded.getId());
        assertEquals("Ana", loaded.getAttendee());
        assertEquals("ana@example.com", loaded.getEmail());
        assertEquals(2, loaded.getTickets());
        assertEquals("JAVA-01", loaded.getWorkshop().getCode());
    }

    //Unsuccessful scenario - duplicate reference
    @Test
    void rejectDuplicateReference() {
        registrations.saveAndFlush(new Registration("REF-1", "Ana", "ana@example.com", "C-01", 1, java));

        assertThrows(DataIntegrityViolationException.class, () -> registrations.saveAndFlush(new Registration("REF-1", "Boris", "boris@example.com", "C-01", 1, java)));
    }

    //Unsuccessful scenario - duplicate email
    @Test
    void rejectSameEmailInSameWorkshop() {
        registrations.saveAndFlush(new Registration("REF-1", "Ana", "ana@example.com", "C-01", 1, java));

        assertThrows(DataIntegrityViolationException.class, () -> registrations.saveAndFlush(new Registration("REF-2", "Ana", "ana@example.com", "C-01", 1, java)));
    }

    //Unsuccessful scenario - duplicate code (same as setUp code)
    @Test
    void rejectDuplicateWorkshopCode() {
        assertThrows(DataIntegrityViolationException.class, () -> workshops.saveAndFlush(new Workshop("JAVA-01", "Copy", true)));
    }

    //Successful scenario
    @Test
    void findByWorkshopCodeAndAttendeePrefix() {
        Workshop python = workshops.saveAndFlush(new Workshop("PY-01", "Python Workshop", true));
        registrations.saveAndFlush(new Registration("REF-1", "Ana", "ana@example.com", "C-01", 1, java));
        registrations.saveAndFlush(new Registration("REF-2", "Andrej", "andrej@example.com", "C-01", 1, java));
        registrations.saveAndFlush(new Registration("REF-3", "Boris", "boris@example.com", "C-01", 1, java));
        registrations.saveAndFlush(new Registration("REF-4", "Anita", "anita@example.com", "C-01", 1, python));
        em.clear();

        Page<Registration> page = registrations.findByWorkshopCodeAndAttendeeStartingWith("JAVA-01", "An", PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
    }
}