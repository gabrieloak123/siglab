package br.ufrn.dimap.siglab.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class FollowRepository {
    private final JdbcTemplate jdbc;
    public FollowRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void researcher(Long follower, Long followed) {
        jdbc.update("INSERT INTO researcher_follows VALUES (?, ?) ON CONFLICT DO NOTHING", follower, followed);
    }
    public void group(Long follower, String group) {
        jdbc.update("INSERT INTO group_follows VALUES (?, ?) ON CONFLICT DO NOTHING", follower, group);
    }
    public void unfollowResearcher(Long follower, Long followed) {
        jdbc.update("DELETE FROM researcher_follows WHERE follower_id=? AND followed_id=?", follower, followed);
    }
    public void unfollowGroup(Long follower, String group) {
        jdbc.update("DELETE FROM group_follows WHERE follower_id=? AND group_id=?", follower, group);
    }
    public List<Long> researchers(Long follower) {
        return jdbc.queryForList("SELECT followed_id FROM researcher_follows WHERE follower_id=?", Long.class, follower);
    }
    public List<String> groups(Long follower) {
        return jdbc.queryForList("SELECT group_id FROM group_follows WHERE follower_id=?", String.class, follower);
    }
}
