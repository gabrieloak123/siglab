package br.ufrn.dimap.siglab.integration;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdentityClient {
    private final String clientId;
    public GoogleIdentityClient(@Value("${GOOGLE_CLIENT_ID:}") String clientId) { this.clientId = clientId; }
    public Identity verify(String credential) {
        if (clientId.isBlank()) throw new ExternalServiceException("Configure GOOGLE_CLIENT_ID para entrada com Google.");
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
            decoder.setJwtValidator(JwtValidators.createDefault());
            Jwt jwt = decoder.decode(credential);
            String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
            if (!(issuer.equals("https://accounts.google.com") || issuer.equals("accounts.google.com"))
                    || !jwt.getAudience().contains(clientId) || !Boolean.TRUE.equals(jwt.getClaim("email_verified")))
                throw new SecurityException("Credencial Google inválida para esta aplicação.");
            return new Identity(jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"), jwt.getClaimAsString("hd"));
        } catch (JwtException ex) {
            throw new SecurityException("Credencial Google inválida ou expirada.");
        }
    }
    public record Identity(String subject, String email, String name, String hostedDomain) {}
}
