package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.repository.VectorRepository;
import br.ufrn.dimap.siglab.repository.PostLikeRepository;
import br.ufrn.dimap.siglab.repository.FollowRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DiscoveryIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DiscoveryService service;
    @Autowired VectorRepository vectors;
    @Autowired PostLikeRepository likes;
    @Autowired FollowRepository follows;

    @Test void persistsResearcherFollowScopeAndEmbedding() {
        String suffix = UUID.randomUUID().toString();
        Long follower = jdbc.queryForObject("INSERT INTO users(name,email,password,verification_level) VALUES (?,?,?,'NAO_VERIFICADO') RETURNING id", Long.class,
                "Seguidor", "follower-" + suffix + "@example.org", "test");
        Long followed = jdbc.queryForObject("INSERT INTO users(name,email,password,verification_level) VALUES (?,?,?,'NAO_VERIFICADO') RETURNING id", Long.class,
                "Pesquisador", "followed-" + suffix + "@example.org", "test");
        float[] embedding = new float[1024]; embedding[5] = 1;

        follows.researcher(follower, followed, new String[]{"IA na Medicina"}, embedding);

        var saved = follows.researcherFollows(follower);
        assertEquals(1, saved.size());
        assertEquals(List.of("IA na Medicina"), saved.getFirst().interestAreas());
        Double distance = jdbc.queryForObject("SELECT interest_embedding <=> ?::vector FROM researcher_follows WHERE follower_id=? AND followed_id=?",
                Double.class, VectorRepository.literal(embedding), follower, followed);
        assertNotNull(distance);
        assertEquals(0.0, distance, 0.00001);
    }

    @Test void savesMeanSeparatelyAndLeavesDeclaredVectorUnchanged() {
        String suffix = UUID.randomUUID().toString();
        Long user = jdbc.queryForObject("INSERT INTO users(name,email,password,verification_level) VALUES (?,?,?,'NAO_VERIFICADO') RETURNING id", Long.class,
                "Teste descoberta", suffix + "@example.org", "test");
        String group = "discovery-" + suffix;
        jdbc.update("INSERT INTO research_group(group_id,name) VALUES (?,?)", group, "Teste");
        Long first = jdbc.queryForObject("INSERT INTO posts(group_id,title) VALUES (?,?) RETURNING id", Long.class, group, "Um");
        Long second = jdbc.queryForObject("INSERT INTO posts(group_id,title) VALUES (?,?) RETURNING id", Long.class, group, "Dois");
        float[] firstVector = new float[1024]; firstVector[0] = 1;
        float[] secondVector = new float[1024]; secondVector[1] = 1;
        vectors.user(user, firstVector);
        vectors.post(first, firstVector);
        vectors.post(second, secondVector);

        service.save(user, List.of(first, second));
        float[] expected = new float[1024]; expected[0] = 1; expected[1] = 1;
        Double distance = jdbc.queryForObject("SELECT embedding <=> ?::vector FROM user_discovery_vectors WHERE user_id=?", Double.class, VectorRepository.literal(expected), user);
        assertNotNull(distance);
        assertEquals(0.0, distance, 0.00001);
        Double originalDistance = jdbc.queryForObject("SELECT embedding <=> ?::vector FROM user_vectors WHERE user_id=?", Double.class, VectorRepository.literal(firstVector), user);
        assertNotNull(originalDistance);
        assertEquals(0.0, originalDistance, 0.00001);
        assertDoesNotThrow(() -> vectors.suggestions(user));
        assertDoesNotThrow(() -> vectors.suggestedPosts(user, true, 0.65));
    }

    @Test void rebuildsBehaviorVectorFromLikedIndexedPosts() {
        String suffix = UUID.randomUUID().toString();
        Long user = jdbc.queryForObject("INSERT INTO users(name,email,password,verification_level) VALUES (?,?,?,'NAO_VERIFICADO') RETURNING id", Long.class,
                "Teste curtida", suffix + "@example.org", "test");
        String group = "likes-" + suffix;
        jdbc.update("INSERT INTO research_group(group_id,name) VALUES (?,?)", group, "Teste");
        Long post = jdbc.queryForObject("INSERT INTO posts(group_id,title) VALUES (?,?) RETURNING id", Long.class, group, "Curtido");
        float[] vector = new float[1024]; vector[3] = 1;
        vectors.post(post, vector);

        likes.like(user, post);
        likes.refreshVector(user);

        Double distance = jdbc.queryForObject("SELECT embedding <=> ?::vector FROM user_like_vectors WHERE user_id=?", Double.class,
                VectorRepository.literal(vector), user);
        assertNotNull(distance);
        assertEquals(0.0, distance, 0.00001);
        likes.unlike(user, post);
        likes.refreshVector(user);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_like_vectors WHERE user_id=?", Integer.class, user));
    }
}
