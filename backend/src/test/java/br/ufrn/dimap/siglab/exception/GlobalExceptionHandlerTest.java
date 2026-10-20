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
}
