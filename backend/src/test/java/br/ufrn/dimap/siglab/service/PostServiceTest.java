package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.CreatePostRequestDTO;
import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.model.User_ResearchGroup;
import br.ufrn.dimap.siglab.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
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
    @Mock ApplicationEventPublisher events;
    PostService service;
    @BeforeEach void setup() { service=new PostService(posts,groups,users,memberships,indexing,likes,events); }

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
    @Test void publishingAnnouncesTheFollowers() {
        leader();
        CreatePostRequestDTO request=new CreatePostRequestDTO();
        request.setPostType("OUTROS"); request.setContent("Corpo"); request.setTitle("Título");
        when(posts.save(any(Post.class))).thenAnswer(invocation -> { Post post=invocation.getArgument(0); post.setId(9L); return post; });
        service.criar("lab","leader@lab.org",request);
        ArgumentCaptor<Object> event=ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        PostPublishedEvent published=(PostPublishedEvent) event.getValue();
        assertEquals("lab", published.groupId());
        assertEquals(9L, published.postId());
        assertEquals(4L, published.publisherUserId());
        assertEquals("Título", published.title());
    }
    @Test void anonymousVisitorCanListGroupPosts() {
        when(groups.existsById("lab")).thenReturn(true);
        when(posts.findByGroupIdOrderByCreatedAtDesc("lab")).thenReturn(List.of(Post.builder().id(1L).groupId("lab").title("Post").build()));
        List<Post> found=service.listar("lab",null);
        assertEquals(1,found.size());
        verifyNoInteractions(users);
    }
    @Test void anonymousVisitorCanReadASingleGroupPost() {
        when(groups.existsById("lab")).thenReturn(true);
        when(posts.findByIdAndGroupId(3L,"lab")).thenReturn(Optional.of(Post.builder().id(3L).groupId("lab").title("Post").build()));
        assertEquals(3L,service.buscar("lab",3L,null).getId());
        verifyNoInteractions(users);
    }
    @Test void unknownEmailIsRejectedWhenListingPosts() {
        when(groups.existsById("lab")).thenReturn(true);
        when(users.existsByEmail("desconhecido@lab.org")).thenReturn(false);
        assertThrows(SecurityException.class, () -> service.listar("lab","desconhecido@lab.org"));
        verifyNoInteractions(posts);
    }
    @Test void missingGroupIsReportedToAnonymousVisitors() {
        when(groups.existsById("lab")).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.listar("lab",null));
    }
}
