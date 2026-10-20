package br.ufrn.dimap.siglab.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class VectorRepository {
    public record Suggestion(String type, String id) {}
    private final JdbcTemplate jdbc;
    public VectorRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public static String literal(float[] vector) {
        if (vector == null || vector.length != 1024) throw new IllegalArgumentException("Vetor deve ter 1024 dimensões.");
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (!Float.isFinite(vector[i])) throw new IllegalArgumentException("Vetor contém valor não finito.");
            if (i > 0) out.append(',');
            out.append(vector[i]);
        }
        return out.append(']').toString();
    }

    public void user(Long id, float[] vector) {
        jdbc.update("INSERT INTO user_vectors(user_id, embedding) VALUES (?, ?::vector) ON CONFLICT(user_id) DO UPDATE SET embedding=EXCLUDED.embedding", id, literal(vector));
    }
    public void group(String id, float[] vector) {
        jdbc.update("INSERT INTO group_vectors(group_id, embedding) VALUES (?, ?::vector) ON CONFLICT(group_id) DO UPDATE SET embedding=EXCLUDED.embedding", id, literal(vector));
    }
    public void post(Long id, float[] vector) {
        jdbc.update("INSERT INTO post_vectors(post_id, embedding) VALUES (?, ?::vector) ON CONFLICT(post_id) DO UPDATE SET embedding=EXCLUDED.embedding", id, literal(vector));
    }
    public List<Long> missingUsers() {
        return jdbc.queryForList("SELECT u.id FROM users u LEFT JOIN user_vectors v ON v.user_id=u.id WHERE v.user_id IS NULL", Long.class);
    }
    public List<String> missingGroups() {
        return jdbc.queryForList("SELECT g.group_id FROM research_group g LEFT JOIN group_vectors v ON v.group_id=g.group_id WHERE v.group_id IS NULL", String.class);
    }
    public List<Long> missingPosts() {
        return jdbc.queryForList("SELECT p.id FROM posts p LEFT JOIN post_vectors v ON v.post_id=p.id WHERE v.post_id IS NULL", Long.class);
    }
    public List<Long> suggestedResearchers(Long userId) {
        return suggestions(userId).stream().filter(s -> s.type().equals("researcher"))
                .map(s -> Long.valueOf(s.id())).toList();
    }
    public List<String> suggestedGroups(Long userId) {
        return suggestions(userId).stream().filter(s -> s.type().equals("group")).map(Suggestion::id).toList();
    }
    public List<Suggestion> suggestions(Long userId) {
        return jdbc.query("""
            SELECT type, id FROM (
              SELECT 'researcher' AS type, u.id::text AS id, v.embedding <=> mine.embedding AS distance
              FROM users u JOIN user_vectors v ON v.user_id=u.id JOIN user_vectors mine ON mine.user_id=?
              WHERE u.id<>? AND NOT EXISTS(SELECT 1 FROM researcher_follows f WHERE f.follower_id=? AND f.followed_id=u.id)
              UNION ALL
              SELECT 'group', g.group_id, v.embedding <=> mine.embedding AS distance
              FROM research_group g JOIN group_vectors v ON v.group_id=g.group_id JOIN user_vectors mine ON mine.user_id=?
              WHERE NOT EXISTS(SELECT 1 FROM group_follows f WHERE f.follower_id=? AND f.group_id=g.group_id)
                AND NOT EXISTS(SELECT 1 FROM user_research_group member JOIN users member_user ON member.user_email=member_user.email
                               WHERE member.group_id=g.group_id AND member_user.id=?)
            ) candidates ORDER BY distance, type, id LIMIT 3
            """, (rs, row) -> new Suggestion(rs.getString("type"), rs.getString("id")),
                userId, userId, userId, userId, userId, userId);
    }
    public List<Long> suggestedPosts(Long userId, boolean followedOnly, double maxFollowedDistance) {
        if (maxFollowedDistance < 0 || maxFollowedDistance > 2)
            throw new IllegalArgumentException("Distância máxima deve estar entre 0 e 2.");
        return jdbc.queryForList("""
            SELECT p.id FROM posts p JOIN post_vectors v ON v.post_id=p.id
            JOIN user_vectors mine ON mine.user_id=?
            WHERE ? = false OR (
              (EXISTS(SELECT 1 FROM researcher_follows f WHERE f.follower_id=? AND f.followed_id=p.publisher_user_id)
               OR EXISTS(SELECT 1 FROM group_follows f WHERE f.follower_id=? AND f.group_id=p.group_id))
              AND (v.embedding <=> mine.embedding) <= ?
            )
            ORDER BY v.embedding <=> mine.embedding, p.created_at DESC, p.id DESC
            """, Long.class, userId, followedOnly, userId, userId, maxFollowedDistance);
    }
}
