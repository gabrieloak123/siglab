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
    /** Seguidor com os dados necessários para receber notificações por e-mail. */
    public record Follower(Long id, String email, String name, String[] interestAreas) {}
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
        if (value == null) return List.of();
        Object raw = value.getArray();
        if (!(raw instanceof Object[] items)) return List.of();
        return java.util.Arrays.stream(items).filter(java.util.Objects::nonNull).map(Object::toString).toList();
    }
    public List<String> groups(Long follower) {
        return jdbc.queryForList("SELECT group_id FROM group_follows WHERE follower_id=?", String.class, follower);
    }
    /**
     * Lista quem segue um grupo de pesquisa, com os dados de contato do seguidor.
     *
     * @param groupId o identificador do grupo de pesquisa
     * @return a lista de seguidores do grupo
     */
    public List<Follower> groupFollowers(String groupId) {
        return jdbc.query("""
            SELECT u.id AS follower_id, u.email, u.name
            FROM group_follows f JOIN users u ON u.id=f.follower_id
            WHERE f.group_id=? ORDER BY u.id
            """, (rs, row) -> new Follower(rs.getLong("follower_id"), rs.getString("email"), rs.getString("name"), new String[0]),
                groupId);
    }
    /**
     * Lista quem segue um pesquisador, com as áreas de interesse escolhidas no seguimento.
     *
     * @param followedId o identificador do pesquisador seguido
     * @return a lista de seguidores do pesquisador
     */
    public List<Follower> researcherFollowers(Long followedId) {
        return jdbc.query("""
            SELECT u.id AS follower_id, u.email, u.name, f.interest_areas
            FROM researcher_follows f JOIN users u ON u.id=f.follower_id
            WHERE f.followed_id=? ORDER BY u.id
            """, (rs, row) -> new Follower(rs.getLong("follower_id"), rs.getString("email"), rs.getString("name"),
                strings(rs.getArray("interest_areas")).toArray(String[]::new)), followedId);
    }
}
