package br.ufrn.dimap.siglab.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostLikeRepository {
    private final JdbcTemplate jdbc;
    public PostLikeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void like(Long userId, Long postId) {
        jdbc.update("INSERT INTO post_likes(user_id,post_id) VALUES (?,?) ON CONFLICT DO NOTHING", userId, postId);
    }
    public void unlike(Long userId, Long postId) {
        jdbc.update("DELETE FROM post_likes WHERE user_id=? AND post_id=?", userId, postId);
    }
    public boolean liked(Long userId, Long postId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM post_likes WHERE user_id=? AND post_id=?)", Boolean.class, userId, postId));
    }
    public long count(Long postId) {
        Long count = jdbc.queryForObject("SELECT count(*) FROM post_likes WHERE post_id=?", Long.class, postId);
        return count == null ? 0 : count;
    }
    public void refreshVector(Long userId) {
        jdbc.update("DELETE FROM user_like_vectors WHERE user_id=?", userId);
        jdbc.update("""
            INSERT INTO user_like_vectors(user_id,embedding)
            SELECT ?,l2_normalize(avg(l2_normalize(v.embedding)))
            FROM post_likes l JOIN post_vectors v ON v.post_id=l.post_id
            WHERE l.user_id=? AND vector_norm(v.embedding)>0
            HAVING count(*)>0 AND vector_norm(avg(l2_normalize(v.embedding)))>0
            """, userId, userId);
    }
    public void refreshVectorsForPost(Long postId) {
        for (Long userId : usersForPost(postId))
            refreshVector(userId);
    }
    public java.util.List<Long> usersForPost(Long postId) {
        return jdbc.queryForList("SELECT user_id FROM post_likes WHERE post_id=?", Long.class, postId);
    }
}
