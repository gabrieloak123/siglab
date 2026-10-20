package br.ufrn.dimap.siglab.integration;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import br.ufrn.dimap.siglab.dto.ScholarCandidate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import java.util.Map;

@Component
public class ScholarClient implements ResearchProfileProvider {
    private final String apiKey;
    private final RestClient client;
    private final String searchUrl;

    public ScholarClient(@Value("${SERPAPI_KEY:}") String apiKey,
                         @Value("${SERPAPI_SEARCH_URL:https://serpapi.com/search.json}") String searchUrl,
                         RestClient client) { this.apiKey = apiKey; this.searchUrl = searchUrl; this.client = client; }

    public List<String> interests(String authorId) {
        return profile(authorId).interests();
    }
    public ScholarCandidate profile(String authorId) {
        if (apiKey.isBlank()) throw new ExternalServiceException("Configure SERPAPI_KEY para consultar o Google Scholar.");
        try {
            String uri = UriComponentsBuilder.fromUriString(searchUrl)
                    .queryParam("engine", "google_scholar_author")
                    .queryParam("author_id", authorId).queryParam("api_key", apiKey).build().toUriString();
            Map<?, ?> response = client.get().uri(uri).retrieve().body(Map.class);
            if (response == null || !(response.get("author") instanceof Map<?, ?> author))
                throw new IllegalArgumentException("Perfil Google Scholar não encontrado para o author_id informado.");
            List<String> interests = extractInterests(author.get("interests"));
            return new ScholarCandidate(authorId, string(author.get("name")), string(author.get("affiliations")),
                    domain(string(author.get("email"))), interests);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Falha ao consultar Google Scholar via SerpApi.", ex);
        }
    }
    public List<ScholarCandidate> searchProfiles(String name) {
        if (apiKey.isBlank()) throw new ExternalServiceException("Configure SERPAPI_KEY para pesquisar perfis Google Scholar.");
        try {
            String uri=UriComponentsBuilder.fromUriString(searchUrl)
                    .queryParam("engine", "google_scholar_profiles").queryParam("mauthors", name)
                    .queryParam("api_key", apiKey).build().toUriString();
            Map<?,?> response=client.get().uri(uri).retrieve().body(Map.class);
            if (response == null || !(response.get("profiles") instanceof List<?> profiles)) return List.of();
            return profiles.stream().filter(Map.class::isInstance).map(Map.class::cast).limit(5)
                    .map(p -> new ScholarCandidate(string(p.get("author_id")), string(p.get("name")), string(p.get("affiliations")),
                            domain(string(p.get("email"))), extractInterests(p.get("interests")))).toList();
        } catch (RestClientException ex) { throw new ExternalServiceException("Falha ao pesquisar perfis Google Scholar.", ex); }
    }
    private List<String> extractInterests(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(Map.class::cast)
                    .map(item -> item.get("title")).filter(String.class::isInstance)
                    .map(String.class::cast).toList();
    }
    private String string(Object value) { return value instanceof String text ? text : ""; }
    private String domain(String emailLabel) {
        int at=emailLabel.toLowerCase().lastIndexOf(" at ");
        return at < 0 ? "" : emailLabel.substring(at + 4).trim().toLowerCase();
    }
}
