package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.service.PostService;
import br.ufrn.dimap.siglab.service.SessionUser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostControllerTest {
    private final PostService posts = mock(PostService.class);
    private final SessionUser session = mock(SessionUser.class);
    private final PostController controller = new PostController(posts, session);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test void listsGroupPostsForAnonymousVisitors() {
        when(session.find(request)).thenReturn(Optional.empty());
        when(posts.listar("lab", null)).thenReturn(List.of(Post.builder().id(1L).build()));
        assertEquals(1, controller.list("lab", request).size());
    }

    @Test void readsTheGroupPostOfAnAnonymousVisitor() {
        when(session.find(request)).thenReturn(Optional.empty());
        when(posts.buscar("lab", 7L, null)).thenReturn(Post.builder().id(7L).build());
        assertEquals(7L, controller.get("lab", 7L, request).getId());
    }

    @Test void usesTheSessionUserWhenAuthenticated() {
        when(session.find(request)).thenReturn(Optional.of(User.builder().email("leitor@lab.org").build()));
        controller.list("lab", request);
        verify(posts).listar("lab", "leitor@lab.org");
    }
}
