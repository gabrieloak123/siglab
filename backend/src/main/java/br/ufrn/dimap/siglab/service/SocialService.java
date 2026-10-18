package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SocialService {
    private final UserRepository users;
    private final ResearchGroupRepository groups;
    private final PostRepository posts;
    private final VectorRepository vectors;
    private final FollowRepository follows;
    private final IndexBackfillService backfill;
    public SocialService(UserRepository users, ResearchGroupRepository groups, PostRepository posts,
                         VectorRepository vectors, FollowRepository follows, IndexBackfillService backfill) {
        this.users=users; this.groups=groups; this.posts=posts; this.vectors=vectors; this.follows=follows; this.backfill=backfill;
    }
    public Long userId(Object value) {
        if (!(value instanceof Long id) || !users.existsById(id)) throw new SecurityException("Entre para continuar.");
        return id;
    }
    @Transactional
    public void followResearcher(Long me, Long target) {
        if (me.equals(target)) throw new IllegalArgumentException("Não é possível seguir a si mesmo.");
        if (!users.existsById(target)) throw new ResourceNotFoundException("Pesquisador não encontrado.");
        follows.researcher(me, target);
    }
    @Transactional
    public void followGroup(Long me, String target) {
        if (!groups.existsById(target)) throw new ResourceNotFoundException("Grupo não encontrado.");
        follows.group(me, target);
    }
    @Transactional
    public void unfollowResearcher(Long me, Long target) { follows.unfollowResearcher(me, target); }
    @Transactional
    public void unfollowGroup(Long me, String target) { follows.unfollowGroup(me, target); }
    public List<User> researchers(Long me) { backfill.ensureProfiles(); return vectors.suggestedResearchers(me).stream().map(users::findById).flatMap(java.util.Optional::stream).toList(); }
    public List<ResearchGroup> groups(Long me) { backfill.ensureProfiles(); return vectors.suggestedGroups(me).stream().map(groups::findById).flatMap(java.util.Optional::stream).toList(); }
    public List<Post> feed(Long me, boolean followedOnly) {
        backfill.ensureProfiles(); backfill.ensurePosts();
        return vectors.suggestedPosts(me, followedOnly).stream().map(posts::findById).flatMap(java.util.Optional::stream).toList();
    }
    public List<Long> followedResearchers(Long me) { return follows.researchers(me); }
    public List<String> followedGroups(Long me) { return follows.groups(me); }
}
