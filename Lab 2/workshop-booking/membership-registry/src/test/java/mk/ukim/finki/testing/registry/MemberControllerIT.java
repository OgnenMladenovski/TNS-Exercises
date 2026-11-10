package mk.ukim.finki.testing.registry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerIT {

    @Autowired MockMvc mvc;

    //Successful scenario - contract for an eligible member
    @Test
    void eligibleMemberContract() throws Exception {
        mvc.perform(get("/members/C-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.memberId").isString())
                .andExpect(jsonPath("$.memberId").value("C-01"))
                .andExpect(jsonPath("$.eligible").isBoolean())
                .andExpect(jsonPath("$.eligible").value(true));
    }

    //Successful scenario - contract for an ineligible member
    @Test
    void ineligibleMemberContract() throws Exception {
        mvc.perform(get("/members/C-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("C-02"))
                .andExpect(jsonPath("$.eligible").isBoolean())
                .andExpect(jsonPath("$.eligible").value(false));
    }

    //Unsuccessful scenario - member does not exist (404)
    @Test
    void unknownMemberContract() throws Exception {
        mvc.perform(get("/members/C-99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }
}