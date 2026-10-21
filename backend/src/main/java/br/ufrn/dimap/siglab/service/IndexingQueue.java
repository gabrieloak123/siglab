package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.integration.EmbeddingProvider;
import br.ufrn.dimap.siglab.integration.KeywordExtractor;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.repository.PostRepository;
import br.ufrn.dimap.siglab.repository.PostLikeRepository;
import br.ufrn.dimap.siglab.repository.ResearchGroupRepository;
import br.ufrn.dimap.siglab.repository.UserRepository;
import br.ufrn.dimap.siglab.repository.VectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** Fila persistente: chamadas externas ocorrem fora das transações de domínio. */
@Service
public class IndexingQueue {
    private static final Logger log = LoggerFactory.getLogger(IndexingQueue.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final UserRepository users;
    private final ResearchGroupRepository groups;
    private final PostRepository posts;
    private final VectorRepository vectors;
    private final EmbeddingProvider embeddings;
    private final KeywordExtractor keywords;
    private final PostLikeRepository likes;

    public IndexingQueue(JdbcTemplate jdbc, TransactionTemplate transaction, UserRepository users,
                         ResearchGroupRepository groups, PostRepository posts, VectorRepository vectors,
                         EmbeddingProvider embeddings, KeywordExtractor keywords,
                         PostLikeRepository likes) {
        this.jdbc = jdbc; this.transaction = transaction; this.users = users; this.groups = groups;
        this.posts = posts; this.vectors = vectors; this.embeddings = embeddings; this.keywords = keywords; this.likes = likes;
    }

    /** Deve ser chamado na mesma transação que salva a entidade. */
    public void enqueue(String type, String id) {
        String deleteSql = switch (type) {
            case "USER" -> "DELETE FROM user_vectors WHERE user_id=?::bigint";
            case "GROUP" -> "DELETE FROM group_vectors WHERE group_id=?";
            case "POST" -> "DELETE FROM post_vectors WHERE post_id=?::bigint";
            default -> throw new IllegalArgumentException("Tipo de indexação inválido.");
        };
        jdbc.update("""
            INSERT INTO indexing_jobs(entity_type, entity_id) VALUES (?, ?)
            ON CONFLICT(entity_type, entity_id) DO UPDATE SET version=indexing_jobs.version+1,
              status='PENDING', attempts=0, available_at=now(), lease_until=NULL
            """, type, id);
        jdbc.update(deleteSql, id);
        if ("POST".equals(type)) likes.refreshVectorsForPost(Long.valueOf(id));
    }

    /** Descobre dados legados e seeds sem vetor, sem reabrir trabalhos que falharam. */
    @Scheduled(initialDelayString = "${INDEX_DISCOVERY_INITIAL_DELAY_MS:30000}", fixedDelayString = "${INDEX_DISCOVERY_DELAY_MS:60000}")
    public void discoverMissing() {
        jdbc.update("""
            INSERT INTO indexing_jobs(entity_type, entity_id)
            SELECT 'USER', u.id::text FROM users u LEFT JOIN user_vectors v ON v.user_id=u.id
            WHERE v.user_id IS NULL AND cardinality(u.interest_areas)=3 ON CONFLICT DO NOTHING
            """);
        jdbc.update("""
            INSERT INTO indexing_jobs(entity_type, entity_id)
            SELECT 'GROUP', g.group_id FROM research_group g LEFT JOIN group_vectors v ON v.group_id=g.group_id
            WHERE v.group_id IS NULL AND cardinality(g.research_fields)=3 ON CONFLICT DO NOTHING
            """);
        jdbc.update("""
            INSERT INTO indexing_jobs(entity_type, entity_id)
            SELECT 'POST', p.id::text FROM posts p LEFT JOIN post_vectors v ON v.post_id=p.id
            WHERE v.post_id IS NULL ON CONFLICT DO NOTHING
            """);
    }

    private record Job(String type, String id, long version) {}
    private record Prepared(float[] vector, String[] generatedKeywords) {}

    @Scheduled(initialDelayString = "${INDEX_WORKER_INITIAL_DELAY_MS:35000}", fixedDelayString = "${INDEX_WORKER_DELAY_MS:1000}")
    public void processNext() {
        List<Job> jobs = jdbc.query("""
            UPDATE indexing_jobs SET status='PROCESSING', lease_until=now()+interval '5 minutes'
            WHERE (entity_type,entity_id) = (
              SELECT entity_type,entity_id FROM indexing_jobs
              WHERE (status='PENDING' AND available_at<=now()) OR (status='PROCESSING' AND lease_until<now())
              ORDER BY available_at,entity_type,entity_id FOR UPDATE SKIP LOCKED LIMIT 1
            ) RETURNING entity_type,entity_id,version
            """, (rs, n) -> new Job(rs.getString(1), rs.getString(2), rs.getLong(3)));
        if (jobs.isEmpty()) return;
        Job job = jobs.getFirst();
        try {
            Prepared prepared = prepare(job);
            transaction.executeWithoutResult(status -> complete(job, prepared));
        } catch (Exception ex) {
            log.warn("Falha ao indexar {} {}", job.type(), job.id(), ex);
            jdbc.update("""
                UPDATE indexing_jobs SET attempts=attempts+1,
                  status=CASE WHEN attempts+1>=5 THEN 'ERROR' ELSE 'PENDING' END,
                  available_at=now()+(interval '30 seconds' * power(2, LEAST(attempts,4))), lease_until=NULL
                WHERE entity_type=? AND entity_id=? AND version=? AND status='PROCESSING'
                """, job.type(), job.id(), job.version());
        }
    }

    private Prepared prepare(Job job) {
        return switch (job.type()) {
            case "USER" -> users.findById(Long.valueOf(job.id()))
                    .map(user -> new Prepared(embeddings.embed(String.join(", ", user.getInterest_areas())), null)).orElse(null);
            case "GROUP" -> groups.findById(job.id())
                    .map(group -> new Prepared(embeddings.embed(String.join(", ", group.getResearch_fields())), null)).orElse(null);
            case "POST" -> posts.findById(Long.valueOf(job.id())).map(this::preparePost).orElse(null);
            default -> throw new IllegalArgumentException("Tipo de indexação inválido.");
        };
    }

    private Prepared preparePost(Post post) {
        String[] terms = post.getKeywords();
        boolean generated = terms == null || terms.length == 0;
        if (generated) terms = keywords.keywords(post.getContent() == null ? post.getTitle() : post.getContent());
        String summary = post.getDescription();
        if (summary == null || summary.isBlank()) summary = post.getContent();
        String text = (String.join(", ", terms) + "\n" + (summary == null ? "" : summary.trim())).trim();
        return new Prepared(embeddings.embed(text), generated ? terms : null);
    }

    private void complete(Job job, Prepared prepared) {
        List<Long> versions = jdbc.queryForList("SELECT version FROM indexing_jobs WHERE entity_type=? AND entity_id=? AND status='PROCESSING' FOR UPDATE",
                Long.class, job.type(), job.id());
        if (versions.isEmpty() || versions.getFirst() != job.version()) return;
        if (prepared != null) {
            switch (job.type()) {
                case "USER" -> vectors.user(Long.valueOf(job.id()), prepared.vector());
                case "GROUP" -> vectors.group(job.id(), prepared.vector());
                case "POST" -> {
                    if (prepared.generatedKeywords() != null) posts.findById(Long.valueOf(job.id())).ifPresent(post -> {
                        post.setKeywords(prepared.generatedKeywords());
                        posts.save(post);
                    });
                    vectors.post(Long.valueOf(job.id()), prepared.vector());
                    likes.refreshVectorsForPost(Long.valueOf(job.id()));
                }
                default -> throw new IllegalArgumentException("Tipo de indexação inválido.");
            }
        }
        jdbc.update("DELETE FROM indexing_jobs WHERE entity_type=? AND entity_id=? AND version=?", job.type(), job.id(), job.version());
    }
}
