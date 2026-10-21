package br.ufrn.dimap.siglab.integration;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingClient implements EmbeddingProvider {
    private final String accountId;
    private final String token;
    private final RestClient client;
    private final String modelUrl;
    private static final int BATCH_SIZE = 16;

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
        return embedBatch(List.of(text)).getFirst();
    }

    /**
     * Gera embeddings bge-m3 (1024 dimensões) para vários textos, em lotes.
     * A ordem da lista de retorno corresponde exatamente à ordem de {@code texts}.
     *
     * @throws ExternalServiceException se a Cloudflare estiver mal configurada ou retornar vetores inválidos
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (accountId.isBlank() || token.isBlank())
            throw new ExternalServiceException("Configure CLOUDFLARE_ACCOUNT_ID e CLOUDFLARE_API_TOKEN.");
        if (texts == null || texts.isEmpty()) return List.of();
        List<float[]> out = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += BATCH_SIZE)
            out.addAll(invoke(texts.subList(start, Math.min(start + BATCH_SIZE, texts.size()))));
        return out;
    }

    private List<float[]> invoke(List<String> batch) {
        try {
            Map<?, ?> response = client.post().uri(modelUrl, accountId)
                    .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", batch)).retrieve().body(Map.class);
            if (response == null || !(response.get("result") instanceof Map<?, ?> result)
                    || !(result.get("data") instanceof List<?> data) || data.size() != batch.size())
                throw new ExternalServiceException("Cloudflare retornou embeddings inválidos; esperado bge-m3 com 1024 dimensões.");
            List<float[]> vectors = new ArrayList<>(data.size());
            for (Object raw : data) vectors.add(toVector(raw));
            return vectors;
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Falha ao gerar embeddings na Cloudflare.", ex);
        }
    }

    private float[] toVector(Object raw) {
        if (!(raw instanceof List<?> vector) || vector.size() != 1024)
            throw new ExternalServiceException("Cloudflare retornou um vetor inválido; esperado bge-m3 com 1024 dimensões.");
        float[] values = new float[1024];
        for (int i = 0; i < values.length; i++) {
            if (!(vector.get(i) instanceof Number number)) throw new ExternalServiceException("Vetor inválido da Cloudflare.");
            values[i] = number.floatValue();
        }
        return values;
    }
}
