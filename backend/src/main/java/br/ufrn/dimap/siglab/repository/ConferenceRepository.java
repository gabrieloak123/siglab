package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.dto.ConferenceSuggestionDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Cache local (via JDBC) das conferências da Conference Partner, com embeddings pgvector
 * no mesmo espaço vetorial de 1024 dimensões usado por pesquisadores, grupos e posts —
 * o que permite ordenar por proximidade com o usuário com o mesmo operador de cosseno
 * ({@code <=>}) das demais sugestões do SIGLab.
 * <p>
 * Os dados são um cache somente-leitura, atualizado por campo a cada 24h
 * ({@code conference_field_refresh}); o custo é de no máximo uma requisição à API
 * por campo/dia.
 * </p>
 *
 */
@Repository
public class ConferenceRepository {

    private final JdbcTemplate jdbc;

    public ConferenceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Uma conferência do cache, alinhada ao seu embedding. */
    public record StoredVenue(long id, String field, String shortName, String fullName,
                              LocalDate submissionDate, LocalDate conferenceDate, String location,
                              String ccfRank, String detailPage, Double cpIndexScore, float[] embedding) {}

    /** Última atualização do cache para um campo/chave de consulta, se houver. */
    public Optional<LocalDateTime> refreshedAt(String field) {
        return Optional.ofNullable(jdbc.query(
                "SELECT refreshed_at FROM conference_field_refresh WHERE field = ?",
                rs -> rs.next() ? rs.getTimestamp(1).toLocalDateTime() : null, field));
    }

    /**
     * Substitui atomicamente o snapshot de um campo: apaga as linhas e embeddings antigos,
     * insere os novos e registra o marcador de frescor.
     */
    @Transactional
    public void replaceSnapshot(String field, List<StoredVenue> snapshot) {
        jdbc.update("DELETE FROM conferences WHERE field = ?", field);
        for (StoredVenue venue : snapshot) {
            jdbc.update("""
                INSERT INTO conferences(id, field, short_name, full_name, submission_date,
                                        conference_date, location, ccf_rank, detail_page, cp_index_score)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, venue.id(), venue.field(), venue.shortName(), venue.fullName(),
                    venue.submissionDate(), venue.conferenceDate(), venue.location(),
                    venue.ccfRank(), venue.detailPage(), venue.cpIndexScore());
            jdbc.update("""
                INSERT INTO conference_vectors(conference_id, embedding) VALUES (?, ?::vector)
                ON CONFLICT (conference_id) DO UPDATE SET embedding = EXCLUDED.embedding
                """, venue.id(), VectorRepository.literal(venue.embedding()));
        }
        jdbc.update("""
            INSERT INTO conference_field_refresh(field, refreshed_at) VALUES (?, ?)
            ON CONFLICT (field) DO UPDATE SET refreshed_at = EXCLUDED.refreshed_at
            """, field, LocalDateTime.now());
    }

    /** Indica se o usuário já possui embedding no índice de perfis. */
    public boolean hasUserVector(Long userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM user_vectors WHERE user_id = ?)", Boolean.class, userId));
    }

    /**
     * Sugestões ordenadas por proximidade de cosseno com o perfil do usuário
     * (mesmo operador {@code <=>} de "Pesquisadores sugeridos"), restritas a
     * conferências futuras dos campos informados.
     *
     * @param userId  usuário dono do perfil de referência
     * @param fields  chaves de campo consultadas (slugs ou {@code q:<área>})
     * @param limit   máximo de sugestões
     * @param similar {@code true} para ordenar por embedding; {@code false} (perfil sem
     *                vetor) ordena pela data do evento — mais próximo primeiro
     */
    public List<ConferenceSuggestionDTO> ranked(Long userId, List<String> fields, int limit, boolean similar) {
        if (fields == null || fields.isEmpty()) return List.of();
        String placeholders = String.join(",", Collections.nCopies(fields.size(), "?"));
        if (similar) {
            String sql = """
                SELECT c.id, c.short_name, c.full_name, c.conference_date, c.submission_date,
                       c.location, c.ccf_rank, c.cp_index_score, c.detail_page
                FROM conferences c
                JOIN conference_vectors v ON v.conference_id = c.id
                JOIN user_vectors mine ON mine.user_id = ?
                WHERE c.field IN (%s) AND c.conference_date >= CURRENT_DATE
                ORDER BY v.embedding <=> mine.embedding
                LIMIT ?""".formatted(placeholders);
            Object[] params = new Object[fields.size() + 2];
            params[0] = userId;
            System.arraycopy(fields.toArray(), 0, params, 1, fields.size());
            params[params.length - 1] = limit;
            return jdbc.query(sql, ConferenceRepository::mapRow, params);
        }
        // Perfil sem vetor de referência (áreas de interesse incompletas): ordem por data —
        // o evento mais próximo primeiro, ainda assim excluindo passados.
        String sql = """
            SELECT c.id, c.short_name, c.full_name, c.conference_date, c.submission_date,
                   c.location, c.ccf_rank, c.cp_index_score, c.detail_page
            FROM conferences c
            WHERE c.field IN (%s) AND c.conference_date >= CURRENT_DATE
            ORDER BY c.conference_date ASC
            LIMIT ?""".formatted(placeholders);
        Object[] params = fields.toArray();
        Object[] finalParams = new Object[fields.size() + 1];
        System.arraycopy(params, 0, finalParams, 0, params.length);
        finalParams[fields.size()] = limit;
        return jdbc.query(sql, ConferenceRepository::mapRow, finalParams);
    }

    private static ConferenceSuggestionDTO mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        double score = rs.getDouble("cp_index_score");
        return ConferenceSuggestionDTO.builder()
                .id(rs.getLong("id"))
                .shortName(rs.getString("short_name"))
                .fullName(rs.getString("full_name"))
                .conferenceDate(rs.getObject("conference_date", LocalDate.class))
                .submissionDate(rs.getObject("submission_date", LocalDate.class))
                .location(rs.getString("location"))
                .ccfRank(rs.getString("ccf_rank"))
                .cpIndexScore(rs.wasNull() ? null : score)
                .detailPage(rs.getString("detail_page"))
                .build();
    }
}
