package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.integration.KeywordClient;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndexBackfillService {
    private final UserRepository users;
    private final ResearchGroupRepository groups;
    private final PostRepository posts;
    private final VectorRepository vectors;
    private final InterestService interests;
    private final KeywordClient keywords;
    public IndexBackfillService(UserRepository users, ResearchGroupRepository groups, PostRepository posts,
                                VectorRepository vectors, InterestService interests, KeywordClient keywords) {
        this.users=users; this.groups=groups; this.posts=posts; this.vectors=vectors;
        this.interests=interests; this.keywords=keywords;
    }
    @Transactional
    public void ensureProfiles() {
        for (Long id : vectors.missingUsers()) users.findById(id).ifPresent(user -> {
            if (user.getInterest_areas() != null && user.getInterest_areas().length == 3) interests.index(user);
        });
        for (String id : vectors.missingGroups()) groups.findById(id).ifPresent(group -> {
            if (group.getResearch_fields() != null && group.getResearch_fields().length == 3) interests.index(group);
        });
    }
    @Transactional
    public void ensurePosts() {
        for (Long id : vectors.missingPosts()) posts.findById(id).ifPresent(post -> {
            String[] terms = post.getKeywords();
            if (terms == null || terms.length == 0) {
                terms = keywords.keywords(post.getContent() == null ? post.getTitle() : post.getContent());
                post.setPostType("OUTROS");
                post.setKeywords(terms);
                posts.save(post);
            }
            interests.indexPost(id, terms);
        });
    }
}
