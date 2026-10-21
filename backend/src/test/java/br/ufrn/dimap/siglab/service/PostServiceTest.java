package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.CreatePostRequestDTO;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.model.User_ResearchGroup;
import br.ufrn.dimap.siglab.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock PostRepository posts;
    @Mock ResearchGroupRepository groups;
    @Mock UserRepository users;
    @Mock User_ResearchGroupRepository memberships;
    @Mock IndexingQueue indexing;
    @Mock PostLikeRepository likes;
    PostService service;
    @BeforeEach void setup() { service=new PostService(posts,groups,users,memberships,indexing,likes); }

    private void leader() {
        when(groups.existsById("lab")).thenReturn(true);
        when(users.existsByEmail("leader@lab.org")).thenReturn(true);
        when(users.findByEmail("leader@lab.org")).thenReturn(Optional.of(User.builder().id(4L).build()));
        when(memberships.findById(any())).thenReturn(Optional.of(User_ResearchGroup.builder().isLeader(true).build()));
    }
    @Test void articleRequiresKeywordsBeforePersistence() {
        leader();
        CreatePostRequestDTO request=new CreatePostRequestDTO();
        request.setPostType("ARTIGO"); request.setContent("Corpo");
        assertThrows(IllegalArgumentException.class, () -> service.criar("lab","leader@lab.org",request));
        verifyNoInteractions(posts, indexing);
    }
    @Test void otherPostQueuesKeywordGenerationAndIndexing() {
        leader();
        CreatePostRequestDTO request=new CreatePostRequestDTO();
        request.setPostType("OUTROS"); request.setContent("Corpo"); request.setTitle("Título");
        when(posts.save(any(Post.class))).thenAnswer(invocation -> { Post post=invocation.getArgument(0); post.setId(9L); return post; });
        Post saved=service.criar("lab","leader@lab.org",request);
        assertEquals(0,saved.getKeywords().length);
        verify(indexing).enqueue("POST", "9");
    }
}
