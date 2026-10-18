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
    @Mock IndexBackfillService backfill;
    @Test void refusesSelfFollowWithoutWriting() {
        SocialService service = new SocialService(users, groups, posts, vectors, follows, backfill);
        assertThrows(IllegalArgumentException.class, () -> service.followResearcher(1L,1L));
        verifyNoInteractions(follows);
    }
    @Test void followingExistingResearcherIsIdempotentInRepository() {
        SocialService service = new SocialService(users, groups, posts, vectors, follows, backfill);
        when(users.existsById(2L)).thenReturn(true);
        service.followResearcher(1L,2L);
        verify(follows).researcher(1L,2L);
    }
}
