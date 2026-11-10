package mk.ukim.finki.testing.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpMembershipGatewayIT {

    static final FakeRegistry registry = new FakeRegistry();

    HttpMembershipGateway gateway;

    @BeforeEach
    void setUp() {
        registry.reset();
        gateway = new HttpMembershipGateway(registry.baseUrl(), 500, 500);
    }

    @AfterAll
    static void stopRegistry() {
        registry.stop();
    }

    //Successful scenario
    @Test
    void eligibleMember() {
        assertTrue(gateway.isEligible("C-01"));
        assertEquals("/members/C-01", registry.lastPath());
        assertEquals(1, registry.calls());
    }

    //Successful scenario
    @Test
    void ineligibleMember() {
        registry.respond(200, "{\"memberId\":\"{id}\",\"eligible\":false}");

        assertFalse(gateway.isEligible("C-02"));
    }

    //Unsuccessful scenario - member does not exist (404)
    @Test
    void memberNotFound() {
        registry.respond(404, "{\"code\":\"MEMBER_NOT_FOUND\"}");

        BookingFailure failure = assertThrows(BookingFailure.class, () -> gateway.isEligible("C-99"));
        assertEquals("MEMBER_NOT_FOUND", failure.getCode());
        assertEquals(422, failure.getStatus());
    }

    //Unsuccessful scenario - registry returns server error (500)
    @Test
    void registryServerError() {
        registry.respond(500, "{}");

        BookingFailure failure = assertThrows(BookingFailure.class, () -> gateway.isEligible("C-01"));
        assertEquals("REGISTRY_CONTRACT_ERROR", failure.getCode());
        assertEquals(502, failure.getStatus());
    }

    //Unsuccessful scenario - contract: response is for another member
    @Test
    void wrongMemberIdInResponse() {
        registry.respond(200, "{\"memberId\":\"C-02\",\"eligible\":true}");

        BookingFailure failure = assertThrows(BookingFailure.class, () -> gateway.isEligible("C-01"));
        assertEquals("REGISTRY_CONTRACT_ERROR", failure.getCode());
    }

    //Unsuccessful scenario - contract: eligible has wrong type
    @Test
    void wrongEligibleType() {
        registry.respond(200, "{\"memberId\":\"{id}\",\"eligible\":\"yes\"}");

        BookingFailure failure = assertThrows(BookingFailure.class, () -> gateway.isEligible("C-01"));
        assertEquals("REGISTRY_CONTRACT_ERROR", failure.getCode());
    }

    //Unsuccessful scenario - wrong JSON format
    @Test
    void wrongJsonFormat() {
        registry.respond(200, "{\"memberId\":");

        BookingFailure failure = assertThrows(BookingFailure.class, () -> gateway.isEligible("C-01"));
        assertEquals("REGISTRY_CONTRACT_ERROR", failure.getCode());
    }

    //Unsuccessful scenario - registry responds too slowly
    @Test
    void registryTimeout() {
        registry.delay(1500);

        BookingFailure failure = assertThrows(BookingFailure.class, () -> gateway.isEligible("C-01"));
        assertEquals("REGISTRY_UNAVAILABLE", failure.getCode());
        assertEquals(503, failure.getStatus());
    }

    //Unsuccessful scenario - registry is not running
    @Test
    void registryUnavailable() {
        HttpMembershipGateway offline = new HttpMembershipGateway("http://localhost:1", 500, 500);

        BookingFailure failure = assertThrows(BookingFailure.class, () -> offline.isEligible("C-01"));
        assertEquals("REGISTRY_UNAVAILABLE", failure.getCode());
    }
}