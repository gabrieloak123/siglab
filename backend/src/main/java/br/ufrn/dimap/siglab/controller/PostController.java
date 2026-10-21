package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.CreatePostRequestDTO;
import br.ufrn.dimap.siglab.dto.UpdatePostRequestDTO;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.service.PostService;
import br.ufrn.dimap.siglab.service.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/research-groups/{groupId}/posts")
public class PostController {
    private final PostService posts;
    private final SessionUser sessionUser;
    public PostController(PostService posts, SessionUser sessionUser) {
        this.posts=posts; this.sessionUser=sessionUser;
    }
    @GetMapping
    public List<Post> list(@PathVariable String groupId, HttpServletRequest request) {
        return posts.listar(groupId, sessionUser.require(request).getEmail());
    }
    @GetMapping("/{postId}")
    public Post get(@PathVariable String groupId, @PathVariable Long postId, HttpServletRequest request) {
        return posts.buscar(groupId,postId,sessionUser.require(request).getEmail());
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Post create(@PathVariable String groupId, @Valid @RequestBody CreatePostRequestDTO body, HttpServletRequest request) {
        return posts.criar(groupId,sessionUser.require(request).getEmail(),body);
    }
    @PutMapping("/{postId}")
    public Post update(@PathVariable String groupId, @PathVariable Long postId,
                       @Valid @RequestBody UpdatePostRequestDTO body, HttpServletRequest request) {
        return posts.atualizar(groupId,postId,sessionUser.require(request).getEmail(),body);
    }
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String groupId, @PathVariable Long postId, HttpServletRequest request) {
        posts.excluir(groupId,postId,sessionUser.require(request).getEmail());
    }
}
