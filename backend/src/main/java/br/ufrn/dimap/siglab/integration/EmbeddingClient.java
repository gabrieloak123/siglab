package br.ufrn.dimap.siglab.integration;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingClient implements EmbeddingProvider {
    private final String accountId;
    private final String token;
    private final RestClient client;
    private final String modelUrl;

    public EmbeddingClient(@Value("${CLOUDFLARE_ACCOUNT_ID:}") String accountId,
                           @Value("${CLOUDFLARE_API_TOKEN:}") String token,
                           @Value("${CLOUDFLARE_EMBEDDING_URL:https://api.cloudflare.com/client/v4/accounts/{id}/ai/run/@cf/baai/bge-m3}") String modelUrl,
                           RestClient client) {
        this.accountId = accountId;
        this.token = token;
        this.modelUrl = modelUrl;
        this.client = client;
    }

    public float[] embed(String text) {
        if (accountId.isBlank() || token.isBlank())
            throw new ExternalServiceException("Configure CLOUDFLARE_ACCOUNT_ID e CLOUDFLARE_API_TOKEN.");
        try {
            Map<?, ?> response = client.post().uri(modelUrl, accountId)
                    .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", List.of(text))).retrieve().body(Map.class);
            if (response == null || !(response.get("result") instanceof Map<?, ?> result)
                    || !(result.get("data") instanceof List<?> data) || data.isEmpty()
                    || !(data.getFirst() instanceof List<?> vector) || vector.size() != 1024)
                throw new ExternalServiceException("Cloudflare retornou um vetor inválido; esperado bge-m3 com 1024 dimensões.");
            float[] values = new float[1024];
            for (int i = 0; i < values.length; i++) {
                if (!(vector.get(i) instanceof Number number)) throw new ExternalServiceException("Vetor inválido da Cloudflare.");
                values[i] = number.floatValue();
            }
            return values;
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Falha ao gerar embeddings na Cloudflare.", ex);
        }
    }
}
