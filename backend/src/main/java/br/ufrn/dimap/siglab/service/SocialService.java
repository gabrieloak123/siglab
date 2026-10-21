package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.exception.AuthenticationRequiredException;
import br.ufrn.dimap.siglab.exception.InvalidInterestAreasException;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.*;
import br.ufrn.dimap.siglab.integration.EmbeddingProvider;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SocialService {
    private final UserRepository users;
    private final ResearchGroupRepository groups;
    private final PostRepository posts;
    private final VectorRepository vectors;
    private final FollowRepository follows;
    private final PostLikeRepository likes;
    private final EmbeddingProvider embeddings;
    private final double maxFollowedDistance;
    public SocialService(UserRepository users, ResearchGroupRepository groups, PostRepository posts,
                         VectorRepository vectors, FollowRepository follows, PostLikeRepository likes, EmbeddingProvider embeddings,
                         @Value("${RECOMMENDATION_MAX_DISTANCE:0.65}") double maxFollowedDistance) {
        this.users=users; this.groups=groups; this.posts=posts; this.vectors=vectors; this.follows=follows; this.likes=likes; this.embeddings=embeddings;
        this.maxFollowedDistance=maxFollowedDistance;
    }
    public Long userId(Object value) {
        if (!(value instanceof Long id) || !users.existsById(id)) throw new AuthenticationRequiredException("Entre para continuar.");
        return id;
    }
    @Transactional
    public void followResearcher(Long me, Long target, String[] requestedAreas) {
        if (me.equals(target)) throw new IllegalArgumentException("Não é possível seguir a si mesmo.");
        User researcher=users.findById(target).orElseThrow(() -> new ResourceNotFoundException("Pesquisador não encontrado."));
        String[] areas=validateFollowAreas(requestedAreas, researcher.getInterest_areas());
        follows.researcher(me, target, areas, areas == null ? null : embeddings.embed(String.join(", ", areas)));
    }
    private String[] validateFollowAreas(String[] requested, String[] available) {
        if (requested == null || requested.length == 0) return null;
        var valid=java.util.Arrays.stream(available == null ? new String[0] : available)
                .collect(java.util.stream.Collectors.toMap(a -> a.trim().toLowerCase(java.util.Locale.ROOT), String::trim, (a,b)->a));
        var selected=java.util.Arrays.stream(requested).filter(java.util.Objects::nonNull).map(String::trim).filter(a -> !a.isBlank())
                .map(a -> valid.get(a.toLowerCase(java.util.Locale.ROOT))).filter(java.util.Objects::nonNull).distinct().toArray(String[]::new);
        if (selected.length == 0 || selected.length != java.util.Arrays.stream(requested).filter(java.util.Objects::nonNull).map(String::trim).filter(a -> !a.isBlank()).distinct().count())
            throw new InvalidInterestAreasException("Selecione somente áreas de interesse do pesquisador.");
        return selected;
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
    public List<User> researchers(Long me) { return vectors.suggestedResearchers(me).stream().map(users::findById).flatMap(java.util.Optional::stream).toList(); }
    public List<ResearchGroup> groups(Long me) { return vectors.suggestedGroups(me).stream().map(groups::findById).flatMap(java.util.Optional::stream).toList(); }
    public List<Post> feed(Long me, boolean followedOnly) {
        return vectors.suggestedPosts(me, followedOnly, maxFollowedDistance).stream().map(posts::findById).flatMap(java.util.Optional::stream).toList();
    }
    @Transactional(readOnly = true)
    public List<Post> recentPosts() { return posts.findTop6ByOrderByCreatedAtDesc(); }
    public List<Long> followedResearchers(Long me) { return follows.researchers(me); }
    public List<FollowRepository.ResearcherFollow> researcherFollows(Long me) { return follows.researcherFollows(me); }
    public List<String> followedGroups(Long me) { return follows.groups(me); }
    public record PostLike(boolean liked, long count) {}
    @Transactional(readOnly = true)
    public PostLike postLike(Long me, Long postId) {
        if (!posts.existsById(postId)) throw new ResourceNotFoundException("Post não encontrado.");
        return new PostLike(likes.liked(me, postId), likes.count(postId));
    }
    @Transactional
    public PostLike likePost(Long me, Long postId) {
        if (!posts.existsById(postId)) throw new ResourceNotFoundException("Post não encontrado.");
        likes.like(me, postId);
        likes.refreshVector(me);
        return new PostLike(true, likes.count(postId));
    }
    @Transactional
    public PostLike unlikePost(Long me, Long postId) {
        if (!posts.existsById(postId)) throw new ResourceNotFoundException("Post não encontrado.");
        likes.unlike(me, postId);
        likes.refreshVector(me);
        return new PostLike(false, likes.count(postId));
    }
}
