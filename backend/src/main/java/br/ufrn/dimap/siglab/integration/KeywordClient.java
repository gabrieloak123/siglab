package br.ufrn.dimap.siglab.integration;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class KeywordClient implements KeywordExtractor {
    private final String apiKey;
    private final RestClient client;
    private final String modelUrl;
    public KeywordClient(@Value("${GEMINI_API_KEY:}") String apiKey,
                         @Value("${GEMINI_MODEL_URL:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent}") String modelUrl,
                         RestClient client) { this.apiKey = apiKey; this.modelUrl = modelUrl; this.client = client; }

    public String[] keywords(String content) {
        if (apiKey.isBlank()) throw new ExternalServiceException("Configure GEMINI_API_KEY para classificar posts do tipo Outros.");
        try {
            Map<?, ?> response = client.post().uri(modelUrl)
                    .header("x-goog-api-key", apiKey).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text",
                            "Leia todo o post e responda apenas com três palavras-chave distintas, separadas por vírgulas.\n" + content))))))
                    .retrieve().body(Map.class);
            if (response == null || !(response.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()
                    || !(candidates.getFirst() instanceof Map<?, ?> candidate)
                    || !(candidate.get("content") instanceof Map<?, ?> answer)
                    || !(answer.get("parts") instanceof List<?> parts) || parts.isEmpty()
                    || !(parts.getFirst() instanceof Map<?, ?> part) || !(part.get("text") instanceof String text))
                throw new ExternalServiceException("Gemini não retornou palavras-chave.");
            String[] keywords = Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isBlank()).toArray(String[]::new);
            if (keywords.length != 3) throw new ExternalServiceException("Gemini não retornou exatamente três palavras-chave.");
            return keywords;
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Falha ao classificar o post no Gemini.", ex);
        }
    }
}
