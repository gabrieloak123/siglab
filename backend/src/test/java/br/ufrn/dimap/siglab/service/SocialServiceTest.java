package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.repository.*;
import br.ufrn.dimap.siglab.integration.EmbeddingProvider;
import br.ufrn.dimap.siglab.model.User;
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
    @Mock PostLikeRepository likes;
    @Mock EmbeddingProvider embeddings;
    @Test void refusesSelfFollowWithoutWriting() {
        SocialService service = service();
        assertThrows(IllegalArgumentException.class, () -> service.followResearcher(1L,1L,null));
        verifyNoInteractions(follows);
    }
    @Test void followingExistingResearcherIsIdempotentInRepository() {
        SocialService service = service();
        when(users.findById(2L)).thenReturn(java.util.Optional.of(User.builder().id(2L).interest_areas(new String[]{"IA", "Saúde", "Robótica"}).build()));
        service.followResearcher(1L,2L, null);
        verify(follows).researcher(1L,2L,null,null);
    }
    @Test void followsOnlyAValidatedResearchArea() {
        SocialService service = service();
        var vector=new float[1024];
        when(users.findById(2L)).thenReturn(java.util.Optional.of(User.builder().id(2L).interest_areas(new String[]{"IA na Medicina", "Medicina", "Robótica"}).build()));
        when(embeddings.embed("IA na Medicina")).thenReturn(vector);
        service.followResearcher(1L, 2L, new String[]{"ia na medicina"});
        verify(follows).researcher(eq(1L), eq(2L), argThat(value -> java.util.Arrays.equals(value, new String[]{"IA na Medicina"})), same(vector));
    }
    @Test void refusesAreaThatDoesNotBelongToResearcher() {
        SocialService service = service();
        when(users.findById(2L)).thenReturn(java.util.Optional.of(User.builder().id(2L).interest_areas(new String[]{"Medicina"}).build()));
        assertThrows(IllegalArgumentException.class, () -> service.followResearcher(1L, 2L, new String[]{"IA"}));
        verifyNoInteractions(embeddings, follows);
    }
    @Test void followedFeedUsesSemanticDistanceLimit() {
        SocialService service = service();
        when(vectors.suggestedPosts(1L, true, 0.65)).thenReturn(java.util.List.of());
        assertTrue(service.feed(1L, true).isEmpty());
        verify(vectors).suggestedPosts(1L, true, 0.65);
    }
    @Test void recentPostsDoNotRequireRecommendationBackfill() {
        SocialService service = service();
        when(posts.findTop5ByOrderByCreatedAtDesc()).thenReturn(java.util.List.of());
        assertTrue(service.recentPosts().isEmpty());
        verifyNoInteractions(vectors);
    }
    @Test void likesExistingPostIdempotently() {
        SocialService service = service();
        when(posts.existsById(7L)).thenReturn(true);
        when(likes.count(7L)).thenReturn(3L);
        assertEquals(new SocialService.PostLike(true, 3), service.likePost(1L, 7L));
        verify(likes).like(1L, 7L);
        verify(likes).refreshVector(1L);
    }
    private SocialService service() { return new SocialService(users, groups, posts, vectors, follows, likes, embeddings, 0.65); }
}
