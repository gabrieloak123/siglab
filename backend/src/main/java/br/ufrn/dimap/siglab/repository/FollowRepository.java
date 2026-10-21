package br.ufrn.dimap.siglab.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.sql.Array;

@Repository
public class FollowRepository {
    private final JdbcTemplate jdbc;
    public FollowRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record ResearcherFollow(Long researcherId, List<String> interestAreas) {}
    public void researcher(Long follower, Long followed, String[] areas, float[] embedding) {
        jdbc.update(connection -> {
            var statement=connection.prepareStatement("""
            INSERT INTO researcher_follows(follower_id, followed_id, interest_areas, interest_embedding)
            VALUES (?, ?, ?, ?::vector) ON CONFLICT(follower_id, followed_id) DO UPDATE
            SET interest_areas=EXCLUDED.interest_areas, interest_embedding=EXCLUDED.interest_embedding
            """);
            statement.setLong(1, follower);
            statement.setLong(2, followed);
            if (areas == null) statement.setNull(3, java.sql.Types.ARRAY);
            else statement.setArray(3, connection.createArrayOf("text", areas));
            statement.setString(4, embedding == null ? null : VectorRepository.literal(embedding));
            return statement;
        });
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
    public List<ResearcherFollow> researcherFollows(Long follower) {
        return jdbc.query("SELECT followed_id, interest_areas FROM researcher_follows WHERE follower_id=? ORDER BY followed_id",
                (rs, row) -> new ResearcherFollow(rs.getLong("followed_id"), strings(rs.getArray("interest_areas"))), follower);
    }
    private List<String> strings(Array value) throws java.sql.SQLException {
        return value == null ? List.of() : List.of((String[]) value.getArray());
    }
    public List<String> groups(Long follower) {
        return jdbc.queryForList("SELECT group_id FROM group_follows WHERE follower_id=?", String.class, follower);
    }
}
