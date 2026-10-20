package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {
    @Mock UserRepository users;
    @Mock ResearchGroupRepository groups;
    @Mock PostRepository posts;
    @Mock VectorRepository vectors;
    @Mock FollowRepository follows;
    @Test void refusesSelfFollowWithoutWriting() {
        SocialService service = new SocialService(users, groups, posts, vectors, follows, 0.65);
        assertThrows(IllegalArgumentException.class, () -> service.followResearcher(1L,1L));
        verifyNoInteractions(follows);
    }
    @Test void followingExistingResearcherIsIdempotentInRepository() {
        SocialService service = new SocialService(users, groups, posts, vectors, follows, 0.65);
        when(users.existsById(2L)).thenReturn(true);
        service.followResearcher(1L,2L);
        verify(follows).researcher(1L,2L);
    }
    @Test void followedFeedUsesSemanticDistanceLimit() {
        SocialService service = new SocialService(users, groups, posts, vectors, follows, 0.65);
        when(vectors.suggestedPosts(1L, true, 0.65)).thenReturn(java.util.List.of());
        assertTrue(service.feed(1L, true).isEmpty());
        verify(vectors).suggestedPosts(1L, true, 0.65);
    }
    @Test void recentPostsDoNotRequireRecommendationBackfill() {
        SocialService service = new SocialService(users, groups, posts, vectors, follows, 0.65);
        when(posts.findTop5ByOrderByCreatedAtDesc()).thenReturn(java.util.List.of());
        assertTrue(service.recentPosts().isEmpty());
        verifyNoInteractions(vectors);
    }
}
