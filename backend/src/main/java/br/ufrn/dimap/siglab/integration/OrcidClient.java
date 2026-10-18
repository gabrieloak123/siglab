package br.ufrn.dimap.siglab.integration;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;

@Component
public class OrcidClient {
    private final String clientId, clientSecret, redirectUri, baseUrl;
    private final RestClient client = RestClient.create();
    public OrcidClient(@Value("${ORCID_CLIENT_ID:}") String clientId,
                       @Value("${ORCID_CLIENT_SECRET:}") String clientSecret,
                       @Value("${ORCID_REDIRECT_URI:http://localhost:3000/backend-api/api/auth/orcid/callback}") String redirectUri,
                       @Value("${ORCID_BASE_URL:https://orcid.org}") String baseUrl) {
        this.clientId=clientId; this.clientSecret=clientSecret; this.redirectUri=redirectUri; this.baseUrl=baseUrl;
    }
    public boolean configured() { return !clientId.isBlank() && !clientSecret.isBlank(); }
    public String authorizationUrl(String state) {
        requireConfigured();
        return UriComponentsBuilder.fromUriString(baseUrl + "/oauth/authorize")
                .queryParam("client_id", clientId).queryParam("response_type", "code")
                .queryParam("scope", "openid").queryParam("redirect_uri", redirectUri)
                .queryParam("state", state).build().encode().toUriString();
    }
    public Identity exchange(String code) {
        requireConfigured();
        try {
            Map<?,?> body=client.post().uri(baseUrl + "/oauth/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .body("client_id="+enc(clientId)+"&client_secret="+enc(clientSecret)+"&grant_type=authorization_code&code="+enc(code)+"&redirect_uri="+enc(redirectUri))
                    .retrieve().body(Map.class);
            if (body == null || !(body.get("orcid") instanceof String orcid) || orcid.isBlank())
                throw new ExternalServiceException("ORCID não retornou um identificador autenticado.");
            return new Identity(orcid, body.get("name") instanceof String name ? name : "");
        } catch (RestClientException ex) { throw new ExternalServiceException("Falha ao autenticar com ORCID.", ex); }
    }
    private String enc(String value) { return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8); }
    private void requireConfigured() { if (!configured()) throw new ExternalServiceException("Configure ORCID_CLIENT_ID e ORCID_CLIENT_SECRET."); }
    public record Identity(String orcid, String name) {}
}
