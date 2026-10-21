package br.ufrn.dimap.siglab.integration;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente REST da Conference Partner API (https://www.myhuiban.com/developers).
 * <p>
 * Consulta {@code GET /api/conferences} com filtro de {@code field} (21 áreas de pesquisa)
 * e janela de datas, usando a chave da conta ({@code CONFERENCE_API_KEY}) para valer a cota
 * diária ampliada (200/dia). A busca também funciona sem credencial no modo anônimo
 * (50/dia por IP), então uma chave ausente não é tratada como erro.
 * </p>
 * <p>
 * A API fica atrás do Cloudflare, que rejeita User-Agents padrão de scripts com HTTP 403
 * ("error code: 1010") — por isso um User-Agent descritivo é enviado em toda requisição.
 * </p>
 *
 */
@Component
public class ConferenceClient {

    private static final String BASE_URL = "https://www.myhuiban.com";
    /** Uma página de 100 resultados por campo/consulta é suficiente para as sugestões. */
    private static final int PER_PAGE = 100;

    private final String apiKey;
    private final RestClient client = RestClient.create();

    /** Uma linha de conferência retornada pela busca. */
    public record Venue(long id, String shortName, String fullName, LocalDate submissionDate,
                        LocalDate conferenceDate, String location, String ccfRank,
                        String detailPage, Double cpIndexScore) {}

    /** Uma página de resultados, com o total de páginas informado pela API. */
    public record SearchPage(List<Venue> venues, int totalPages) {}

    public ConferenceClient(@Value("${CONFERENCE_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Busca conferências com data a partir de hoje (exclui eventos passados).
     *
     * @param field             um dos 21 slugs de {@code field} (ex.: {@code ai}); nulo para busca livre
     * @param query             termo de busca no nome da conferência (fallback quando a área de
     *                          interesse não corresponde a nenhum slug); nulo quando {@code field} é usado
     * @param conferenceDateStart data mínima do evento
     * @return as conferências da primeira página (até 100)
     * @throws ExternalServiceException se a chave for inválida, a cota esgotar (402/429) ou a API falhar
     */
    public List<Venue> search(String field, String query, LocalDate conferenceDateStart) {
        return searchPage(field, query, conferenceDateStart, null, 1).venues();
    }

    /**
     * Busca paginada por área e janela de datas (usada pelo formulário de busca).
     *
     * @param field     um dos 21 slugs de {@code field}; nulo para busca livre
     * @param query     termo de busca no nome da conferência; nulo quando {@code field} é usado
     * @param dateStart data mínima do evento
     * @param dateEnd   data máxima do evento; nula para sem limite superior
     * @param page      página (1-based)
     * @return a página de conferências e o total de páginas
     * @throws ExternalServiceException se a chave for inválida, a cota esgotar (402/429) ou a API falhar
     */
    public SearchPage searchPage(String field, String query, LocalDate dateStart, LocalDate dateEnd, int page) {
        try {
            var request = client.get().uri(buildUri(field, query, dateStart, dateEnd, page));
            if (!apiKey.isBlank()) request = request.header("Authorization", "Bearer " + apiKey);
            return parsePage(request.retrieve().body(Map.class));
        } catch (RestClientException ex) {
            throw translate(ex);
        }
    }

    /** Monta a URI já codificada (evita dupla codificação de termos com espaços/acentos). */
    private URI buildUri(String field, String query, LocalDate dateStart, LocalDate dateEnd, int page) {
        var builder = UriComponentsBuilder.fromUriString(BASE_URL + "/api/conferences")
                .queryParam("conference_date_start", dateStart.toString())
                .queryParam("per_page", PER_PAGE)
                .queryParam("page", Math.max(1, page));
        if (dateEnd != null) builder.queryParam("conference_date_end", dateEnd.toString());
        if (field != null && !field.isBlank()) builder.queryParam("field", field);
        if (query != null && !query.isBlank()) builder.queryParam("query", query);
        return builder.build().encode().toUri();
    }

    /** Converte o envelope em uma página, lendo {@code data.pagination.total_pages}. */
    static SearchPage parsePage(Map<?, ?> response) {
        List<Venue> venues = parse(response);
        int totalPages = 1;
        if (response != null && response.get("data") instanceof Map<?, ?> data
                && data.get("pagination") instanceof Map<?, ?> pagination
                && pagination.get("total_pages") instanceof Number pages)
            totalPages = Math.max(1, pages.intValue());
        return new SearchPage(venues, totalPages);
    }

    /** Converte o envelope {@code {code, message, data: {conferences: [...]}}} em {@link Venue}s. */
    static List<Venue> parse(Map<?, ?> response) {
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)
                || !(data.get("conferences") instanceof List<?> rows))
            throw new ExternalServiceException("Conference Partner API retornou uma resposta inesperada.");
        List<Venue> venues = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map)) continue;
            venues.add(new Venue(
                    number(map.get("id")),
                    string(map.get("short_name")),
                    string(map.get("full_name")),
                    date(map.get("submission_date")),
                    date(map.get("conference_date")),
                    string(map.get("location")),
                    string(map.get("ccf_rank")),
                    string(map.get("detail_page")),
                    map.get("cp_index") instanceof Map<?, ?> cp && cp.get("score") instanceof Number score
                            ? score.doubleValue() : null));
        }
        return venues;
    }

    private ExternalServiceException translate(RestClientException ex) {
        if (ex instanceof HttpStatusCodeException http && http.getStatusCode().is4xxClientError()) {
            int status = http.getStatusCode().value();
            if (status == 401) return new ExternalServiceException("Chave CONFERENCE_API_KEY inválida ou revogada.", ex);
            if (status == 402 || status == 429)
                return new ExternalServiceException("Cota diária da Conference Partner API esgotada; tente mais tarde.", ex);
            if (status == 403)
                return new ExternalServiceException("Conference Partner API recusou a requisição (User-Agent).", ex);
        }
        return new ExternalServiceException("Falha ao consultar a Conference Partner API.", ex);
    }

    private static String string(Object value) { return value instanceof String text ? text : null; }

    private static LocalDate date(Object value) {
        return value instanceof String text && !text.isBlank() ? LocalDate.parse(text) : null;
    }

    private static long number(Object value) { return value instanceof Number id ? id.longValue() : 0L; }
}
