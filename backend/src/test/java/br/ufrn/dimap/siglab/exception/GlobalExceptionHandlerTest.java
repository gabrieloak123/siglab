package br.ufrn.dimap.siglab.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    @Test void unexpectedErrorsDoNotExposeInternalDetails() {
        var response = new GlobalExceptionHandler().handleGenericException(
                new RuntimeException("segredo-interno"), new MockHttpServletRequest("GET", "/api/test"));
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getMessage().contains("segredo-interno"));
        assertTrue(response.getBody().getMessage().contains("Referência"));
    }

    @Test void missingAuthenticationReturnsUnauthorized() {
        var response = new GlobalExceptionHandler().handleAuthentication(
                new AuthenticationRequiredException("Sessão necessária"), new MockHttpServletRequest());
        assertEquals(401, response.getStatusCode().value());
    }

    @Test void domainExceptionsReturnTheirExpectedStatus() {
        var handler = new GlobalExceptionHandler();
        var request = new MockHttpServletRequest("GET", "/api/test");
        assertEquals(403, handler.handleAccessDenied(new AccessDeniedException("negado"), request).getStatusCode().value());
        assertEquals(400, handler.handleDomainValidation(new InvalidInterestAreasException("areas"), request).getStatusCode().value());
        assertEquals(400, handler.handleDomainValidation(new InvalidConferenceSearchException("busca"), request).getStatusCode().value());
        assertEquals(409, handler.handleGroupMustHaveLeader(new GroupMustHaveLeaderException("lider"), request).getStatusCode().value());
    }
}
