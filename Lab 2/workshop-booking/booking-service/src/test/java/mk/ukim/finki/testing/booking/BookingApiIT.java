package mk.ukim.finki.testing.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class BookingApiIT {

    @Autowired MockMvc mvc;
    @Autowired RegistrationRepository registrations;
    @Autowired EntityManager em;

    @MockitoBean MembershipGateway membership;

    @BeforeEach
    void setUp() {
        when(membership.isEligible(anyString())).thenReturn(true);
    }

    private String json(String reference, String workshopCode, String memberId, int tickets) {
        return """
                {"reference":"%s","workshopCode":"%s","attendee":" Ana ",
                 "email":"ANA@Example.com","memberId":"%s","tickets":%d}
                """.formatted(reference, workshopCode, memberId, tickets);
    }

    //Successful scenario
    @Test
    void createRegistration() throws Exception {
        mvc.perform(post("/registrations").contentType("application/json")
                        .content(json("REF-1", "JAVA-01", "C-01", 2)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/registrations/REF-1"))
                .andExpect(jsonPath("$.reference").value("REF-1"))
                .andExpect(jsonPath("$.attendee").value("Ana"));

        em.flush();
        em.clear();
        Registration loaded = registrations.findByReference("REF-1").orElseThrow();
        assertEquals("Ana", loaded.getAttendee());
        assertEquals("ana@example.com", loaded.getEmail());
    }

    //Successful scenario - response contract
    @Test
    void getRegistration() throws Exception {
        mvc.perform(post("/registrations").contentType("application/json")
                .content(json("REF-1", "JAVA-01", "C-01", 2)));
        em.flush();
        em.clear();

        mvc.perform(get("/registrations/REF-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.reference").isString())
                .andExpect(jsonPath("$.workshopCode").value("JAVA-01"))
                .andExpect(jsonPath("$.attendee").isString())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.tickets").isNumber());
    }

    //Unsuccessful scenario - invalid input
    @Test
    void rejectInvalidInput() throws Exception {
        mvc.perform(post("/registrations").contentType("application/json")
                        .content(json("REF-1", "JAVA-01", "C-01", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        assertEquals(0, registrations.count());
    }

    //Unsuccessful scenario - invalid JSON
    @Test
    void rejectInvalidJson() throws Exception {
        mvc.perform(post("/registrations").contentType("application/json")
                        .content("{\"reference\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    //Unsuccessful scenario - workshop is closed
    @Test
    void rejectClosedWorkshop() throws Exception {
        mvc.perform(post("/registrations").contentType("application/json")
                        .content(json("REF-1", "CLOSED-01", "C-01", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKSHOP_CLOSED"));

        assertEquals(0, registrations.count());
    }

    //Unsuccessful scenario - member is not eligible
    @Test
    void rejectIneligibleMember() throws Exception {
        when(membership.isEligible("C-02")).thenReturn(false);

        mvc.perform(post("/registrations").contentType("application/json")
                        .content(json("REF-1", "JAVA-01", "C-02", 1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("MEMBER_INELIGIBLE"));

        assertEquals(0, registrations.count());
    }

    //Unsuccessful scenario - registry is unavailable
    @Test
    void rejectWhenRegistryIsUnavailable() throws Exception {
        when(membership.isEligible("C-01")).thenThrow(new BookingFailure(503, "REGISTRY_UNAVAILABLE"));

        mvc.perform(post("/registrations").contentType("application/json")
                        .content(json("REF-1", "JAVA-01", "C-01", 1)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("REGISTRY_UNAVAILABLE"));

        assertEquals(0, registrations.count());
    }

    //Unsuccessful scenario - duplicate reference
    @Test
    void rejectDuplicateReference() throws Exception {
        mvc.perform(post("/registrations").contentType("application/json")
                        .content(json("REF-1", "JAVA-01", "C-01", 1)))
                .andExpect(status().isCreated());

        mvc.perform(post("/registrations").contentType("application/json")
                        .content(json("REF-1", "JAVA-01", "C-01", 1).replace("ANA@", "BORIS@")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    //Unsuccessful scenario - registration does not exist
    @Test
    void returnNotFoundForUnknownReference() throws Exception {
        mvc.perform(get("/registrations/NONE-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REGISTRATION_NOT_FOUND"));
    }
}