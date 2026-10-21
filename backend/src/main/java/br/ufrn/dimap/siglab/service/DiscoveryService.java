package br.ufrn.dimap.siglab.service;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoveryService {
    private final NamedParameterJdbcTemplate jdbc;
    public DiscoveryService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void save(Long userId, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty() || postIds.size() > 30 || postIds.stream().anyMatch(id -> id == null || id <= 0)
                || postIds.stream().distinct().count() != postIds.size())
            throw new IllegalArgumentException("Selecione entre 1 e 30 publicações distintas.");
        var params = Map.of("ids", postIds, "userId", userId);
        int saved = jdbc.update("""
            INSERT INTO user_discovery_vectors(user_id, embedding)
            SELECT :userId, l2_normalize(avg(l2_normalize(embedding))) FROM post_vectors
            WHERE post_id IN (:ids) AND vector_norm(embedding) > 0
            HAVING count(*) = :count AND vector_norm(avg(l2_normalize(embedding))) > 0
            ON CONFLICT(user_id) DO UPDATE SET embedding=excluded.embedding
            """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(params).addValue("count", postIds.size()));
        if (saved != 1) throw new IllegalArgumentException("Uma publicação não está mais disponível para indexação. Selecione novamente.");
    }
}
