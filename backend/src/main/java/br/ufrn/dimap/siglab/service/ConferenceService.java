package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.integration.ConferenceClient;
import br.ufrn.dimap.siglab.integration.ConferenceClient.Venue;
import br.ufrn.dimap.siglab.integration.EmbeddingClient;
import br.ufrn.dimap.siglab.dto.ConferenceSuggestionDTO;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.ConferenceRepository;
import br.ufrn.dimap.siglab.repository.ConferenceRepository.StoredVenue;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recomendação de conferências futuras com base nas três áreas de interesse do perfil.
 * <p>
 * O processo espelha "Pesquisadores sugeridos" / "Grupos sugeridos":
 * </p>
 * <ol>
 *   <li>Cada área de interesse é mapeada para um dos 21 slugs de {@code field} da
 *       Conference Partner API (correspondência por texto, sem diferenciar acentos,
 *       com sinônimos em português e inglês); áreas sem correspondência caem no
 *       parâmetro {@code query} da própria API.</li>
 *   <li>Os resultados (sempre com {@code conference_date >= hoje}, para nunca
 *       recomendar eventos passados) entram no cache local com embedding bge-m3,
 *       no mesmo espaço vetorial dos perfis — atualização a cada 24h por chave,
 *       no máximo uma requisição à API por chave por dia.</li>
 *   <li>A ordenação é busca por similaridade real: {@code embedding <=> embedding_do_usuário}
 *       no pgvector, o mesmo operador de cosseno das demais sugestões.</li>
 * </ol>
 *
 */
@Service
public class ConferenceService {

    private static final Logger log = LoggerFactory.getLogger(ConferenceService.class);

    /** Idade máxima do cache por campo antes de consultar a API novamente. */
    static final Duration CACHE_TTL = Duration.ofHours(24);
    /** Quantidade de conferências sugeridas (a mesma ordem de grandeza das outras sugestões). */
    static final int LIMIT = 6;

    /** Máximo de páginas varridas por busca (100 por página). */
    static final int SEARCH_MAX_PAGES = 5;
    /** Máximo de conferências devolvidas pela busca. */
    static final int SEARCH_LIMIT = 60;
    /** Intervalo máximo aceito entre as datas da busca. */
    static final long SEARCH_MAX_DAYS = 365L * 5;
    /** Cache curto em memória para não repetir buscas idênticas (protege a cota diária). */
    static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(30);
    static final int SEARCH_CACHE_MAX = 200;

    private record SearchKey(String field, LocalDate start, LocalDate end, String country) {}
    private record SearchHit(List<ConferenceSuggestionDTO> items, Instant at) {}

    private final Map<SearchKey, SearchHit> searchCache = new ConcurrentHashMap<>();

    /**
     * As 21 áreas da API na ordem em que são avaliadas — a ordem importa em casos
     * ambíguos (ex.: "redes neurais" deve virar {@code ai}, não {@code network}).
     * Sinônimos já entram normalizados (minúsculas, sem acentos).
     */
    private static final Map<String, List<String>> FIELD_ALIASES = buildAliases();

    private final UserRepository users;
    private final ConferenceClient client;
    private final ConferenceRepository conferences;
    private final EmbeddingClient embeddings;

    public ConferenceService(UserRepository users, ConferenceClient client, ConferenceRepository conferences,
                             EmbeddingClient embeddings) {
        this.users = users;
        this.client = client;
        this.conferences = conferences;
        this.embeddings = embeddings;
    }

    /**
     * Retorna até {@link #LIMIT} conferências futuras próximas dos interesses do usuário.
     *
     * @param userId id do usuário autenticado
     * @throws SecurityException se o usuário não existir (sessão inválida)
     * @throws br.ufrn.dimap.siglab.exception.ExternalServiceException se a Conference Partner
     *         ou a Cloudflare falharem (respondido como HTTP 503)
     */
    public List<ConferenceSuggestionDTO> recommend(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new SecurityException("Entre para continuar."));
        List<String> keys = keys(user.getInterest_areas());
        if (keys.isEmpty()) return List.of();
        ensureFresh(keys);
        boolean similar = conferences.hasUserVector(userId);
        return conferences.ranked(userId, keys, LIMIT, similar);
    }

    /**
     * Busca conferências por área e período, com filtro opcional de país.
     * <p>
     * A Conference Partner não possui filtro por país, então até {@link #SEARCH_MAX_PAGES}
     * páginas são varridas e o país é aplicado sobre a localização. Resultados idênticos
     * ficam em cache por {@link #SEARCH_CACHE_TTL} para preservar a cota diária da API.
     * </p>
     *
     * @param field   um dos 21 slugs de {@code field}
     * @param start   data inicial do período (obrigatória)
     * @param end     data final do período (obrigatória, não anterior à inicial)
     * @param country país opcional (nome ou sinônimo de {@link ConferenceCountries})
     * @return as conferências encontradas, ordenadas pela data do evento
     * @throws IllegalArgumentException se algum filtro for inválido (respondido como HTTP 400)
     */
    public List<ConferenceSuggestionDTO> search(String field, LocalDate start, LocalDate end, String country) {
        validateSearch(field, start, end, country);
        String selectedCountry = country == null || country.isBlank() ? null : country.trim();
        SearchKey key = new SearchKey(field, start, end, selectedCountry);
        SearchHit hit = searchCache.get(key);
        if (hit != null && hit.at().isAfter(Instant.now().minus(SEARCH_CACHE_TTL))) return hit.items();

        Map<Long, Venue> unique = new LinkedHashMap<>();
        for (int page = 1; page <= SEARCH_MAX_PAGES; page++) {
            ConferenceClient.SearchPage result = client.searchPage(field, null, start, end, page);
            result.venues().forEach(venue -> unique.putIfAbsent(venue.id(), venue));
            if (result.venues().isEmpty() || page >= result.totalPages()) break;
        }
        List<ConferenceSuggestionDTO> items = unique.values().stream()
                .filter(venue -> selectedCountry == null
                        || ConferenceCountries.matches(venue.location(), selectedCountry))
                .sorted(Comparator.comparing(Venue::conferenceDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(SEARCH_LIMIT)
                .map(ConferenceService::toDto)
                .toList();

        if (searchCache.size() >= SEARCH_CACHE_MAX) searchCache.clear();
        searchCache.put(key, new SearchHit(items, Instant.now()));
        return items;
    }

    private static void validateSearch(String field, LocalDate start, LocalDate end, String country) {
        if (field == null || !FIELD_ALIASES.containsKey(field))
            throw new IllegalArgumentException("Área de pesquisa inválida.");
        if (start == null || end == null)
            throw new IllegalArgumentException("Informe as datas de início e fim.");
        if (end.isBefore(start))
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à data inicial.");
        if (ChronoUnit.DAYS.between(start, end) > SEARCH_MAX_DAYS)
            throw new IllegalArgumentException("O intervalo máximo de busca é de 5 anos.");
        if (country != null && !country.isBlank() && !ConferenceCountries.isKnown(country))
            throw new IllegalArgumentException("País inválido.");
    }

    private static ConferenceSuggestionDTO toDto(Venue venue) {
        return ConferenceSuggestionDTO.builder()
                .id(venue.id())
                .shortName(venue.shortName())
                .fullName(venue.fullName())
                .conferenceDate(venue.conferenceDate())
                .submissionDate(venue.submissionDate())
                .location(venue.location())
                .ccfRank(venue.ccfRank())
                .cpIndexScore(venue.cpIndexScore())
                .detailPage(venue.detailPage())
                .build();
    }

    /** Mapeia as áreas de interesse para as chaves de consulta (slugs ou {@code q:<área>}). */
    static List<String> keys(String[] areas) {
        if (areas == null) return List.of();
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String area : areas) {
            if (area == null || area.isBlank()) continue;
            String norm = normalize(area);
            if (norm.isEmpty()) continue;
            String slug = matchField(norm);
            keys.add(slug != null ? slug : "q:" + norm);
        }
        return List.copyOf(keys);
    }

    /** Atualiza (no máximo uma vez por dia) cada chave ainda não cacheada ou vencida. */
    void ensureFresh(List<String> keys) {
        LocalDateTime staleBefore = LocalDateTime.now().minus(CACHE_TTL);
        for (String key : keys) {
            boolean stale = conferences.refreshedAt(key)
                    .map(refreshed -> refreshed.isBefore(staleBefore)).orElse(true);
            if (stale) refresh(key);
        }
    }

    /** Busca, embeda e grava o snapshot de uma chave. O marcador de frescor só é gravado no fim. */
    void refresh(String key) {
        boolean byQuery = key.startsWith("q:");
        List<Venue> venues = client.search(byQuery ? null : key, byQuery ? key.substring(2) : null, LocalDate.now());
        List<String> texts = venues.stream()
                .map(venue -> (venue.shortName() + " " + venue.fullName()).trim()).toList();
        List<float[]> vectors = embeddings.embedBatch(texts);
        List<StoredVenue> snapshot = new ArrayList<>(venues.size());
        for (int i = 0; i < venues.size(); i++) {
            Venue venue = venues.get(i);
            snapshot.add(new StoredVenue(venue.id(), key, venue.shortName(), venue.fullName(),
                    venue.submissionDate(), venue.conferenceDate(), venue.location(),
                    venue.ccfRank(), venue.detailPage(), venue.cpIndexScore(), vectors.get(i)));
        }
        conferences.replaceSnapshot(key, snapshot);
        log.info("Cache de conferências atualizado para '{}': {} conferências.", key, snapshot.size());
    }

    /** Encontra o slug de field correspondente à área normalizada, ou {@code null}. */
    static String matchField(String normalizedArea) {
        for (var entry : FIELD_ALIASES.entrySet())
            for (String alias : entry.getValue())
                if (alias.length() >= 3 ? normalizedArea.contains(alias) : normalizedArea.equals(alias))
                    return entry.getKey();
        return null;
    }

    private static String normalize(String value) {
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
        return noAccents.replaceAll("\\s+", " ");
    }

    private static Map<String, List<String>> buildAliases() {
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        aliases.put("ai", List.of("inteligencia artificial", "artificial intelligence", "machine learning",
                "aprendizado de maquina", "deep learning", "redes neurais", "neural", "ia", "ai"));
        aliases.put("vision", List.of("visao computacional", "computer vision", "visao", "image processing",
                "processamento de imagens", "reconhecimento de padroes"));
        aliases.put("nlp", List.of("processamento de linguagem", "natural language",
                "linguistica computacional", "text mining", "nlp", "pln"));
        aliases.put("data", List.of("ciencia de dados", "data science", "data mining", "mineracao de dados",
                "big data", "banco de dados", "database", "data engineering", "estatistica", "dados"));
        aliases.put("network", List.of("redes de computador", "redes", "network", "telecomunicac",
                "internet of things"));
        aliases.put("security", List.of("seguranca", "security", "criptografia", "cryptography",
                "privacidade", "privacy", "cyber"));
        aliases.put("se", List.of("engenharia de software", "software engineering",
                "desenvolvimento de software", "qualidade de software", "software"));
        aliases.put("systems", List.of("sistemas operacionais", "operating systems",
                "arquitetura de computadores", "computer architecture", "sistemas distribuidos",
                "distributed systems", "sistemas embarcados", "embedded systems", "computacao em nuvem",
                "cloud computing", "high performance computing", "virtualizacao", "sistemas"));
        aliases.put("theory", List.of("teoria da computacao", "algoritmos", "algorithms", "complexidade",
                "teoria dos grafos", "grafos", "graph", "logica"));
        aliases.put("hci", List.of("interacao humano", "human-computer", "human computer", "usabilidade",
                "user experience", "experiencia do usuario", "acessibilidade", "interfaces", "interface",
                "ux", "hci"));
        aliases.put("graphics", List.of("graficos", "computer graphics", "visualizacao",
                "realidade virtual", "realidade aumentada", "virtual reality", "augmented reality"));
        aliases.put("bio", List.of("bioinformatica", "biologia", "biology", "genomica", "genetica",
                "computacao bio", "saude", "health", "bio"));
        aliases.put("robotics", List.of("robot", "robos", "automacao", "automation", "autonomous"));
        aliases.put("web", List.of("web semantica", "semantic web", "web", "internet", "e-commerce",
                "comercio eletronico"));
        aliases.put("ee", List.of("engenharia eletrica", "eletronica", "electronics",
                "processamento de sinais", "signal processing", "sistemas eletronicos",
                "microeletronica"));
        aliases.put("energy", List.of("energia", "energy", "power systems", "renovaveis"));
        aliases.put("materials", List.of("materiais", "materials"));
        aliases.put("civil", List.of("engenharia civil", "civil engineering", "geotecnia", "hidraulica", "civil"));
        aliases.put("education", List.of("educacao", "education", "ensino", "aprendizagem",
                "learning analytics", "edtech"));
        aliases.put("management", List.of("gestao", "management", "administracao", "negocios", "business",
                "financas", "finance", "marketing", "economia", "empreendedorismo"));
        aliases.put("math", List.of("matematica", "mathematics", "otimizacao", "optimization",
                "pesquisa operacional", "operations research"));
        return aliases;
    }
}
