package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.CreatePostRequestDTO;
import br.ufrn.dimap.siglab.dto.UpdatePostRequestDTO;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST responsável pelos endpoints de posts (publicações) de um Grupo de Pesquisa.
 * <p>
 * A leitura fica disponível para qualquer usuário cadastrado, enquanto a publicação,
 * edição e exclusão são restritas aos líderes do grupo.
 * </p>
 *
 */
@RestController
@RequestMapping("/api/research-groups/{groupId}/posts")
@CrossOrigin(origins = "*")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Extrai o e-mail do solicitante priorizando o contexto de segurança (SecurityContextHolder).
     * Se o usuário estiver autenticado no Spring Security, seu identificador é utilizado;
     * caso contrário, utiliza o valor do header 'X-Requester-Email' como fallback.
     *
     * @param headerEmail o e-mail informado no header da requisição (opcional)
     * @return o e-mail do solicitante identificado
     */
    private String resolveRequesterEmail(String headerEmail) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return headerEmail;
    }

    /**
     * GET /api/research-groups/{groupId}/posts - Lista os posts do grupo, do mais recente para o mais antigo.
     *
     * @param groupId o ID do grupo de pesquisa
     * @param requesterEmailHeader o e-mail do solicitante no Header {@code X-Requester-Email} (opcional se autenticado)
     * @return a lista de posts do grupo (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<Post>> getPosts(
            @PathVariable("groupId") String groupId,
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmailHeader) {
        List<Post> posts = postService.listar(groupId, resolveRequesterEmail(requesterEmailHeader));
        return ResponseEntity.ok(posts);
    }

    /**
     * GET /api/research-groups/{groupId}/posts/{postId} - Busca um post específico do grupo.
     *
     * @param groupId o ID do grupo de pesquisa
     * @param postId o ID do post
     * @param requesterEmailHeader o e-mail do solicitante no Header {@code X-Requester-Email} (opcional se autenticado)
     * @return o post encontrado (200 OK)
     */
    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPost(
            @PathVariable("groupId") String groupId,
            @PathVariable("postId") Long postId,
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmailHeader) {
        return ResponseEntity.ok(postService.buscar(groupId, postId, resolveRequesterEmail(requesterEmailHeader)));
    }

    /**
     * POST /api/research-groups/{groupId}/posts - Publica um novo post no grupo (Apenas Líder).
     *
     * @param groupId o ID do grupo de pesquisa
     * @param requesterEmailHeader o e-mail do líder solicitante no Header {@code X-Requester-Email} (opcional se autenticado)
     * @param request o corpo da requisição com título, descrição, conteúdo Markdown, tags e nome do arquivo
     * @return o post publicado (201 Created)
     */
    @PostMapping
    public ResponseEntity<Post> createPost(
            @PathVariable("groupId") String groupId,
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmailHeader,
            @Valid @RequestBody CreatePostRequestDTO request) {
        Post post = postService.criar(groupId, resolveRequesterEmail(requesterEmailHeader), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    /**
     * PUT /api/research-groups/{groupId}/posts/{postId} - Atualiza um post existente (Apenas Líder).
     *
     * @param groupId o ID do grupo de pesquisa
     * @param postId o ID do post
     * @param requesterEmailHeader o e-mail do líder solicitante no Header {@code X-Requester-Email} (opcional se autenticado)
     * @param request o corpo da requisição com os novos dados do post
     * @return o post atualizado (200 OK)
     */
    @PutMapping("/{postId}")
    public ResponseEntity<Post> updatePost(
            @PathVariable("groupId") String groupId,
            @PathVariable("postId") Long postId,
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmailHeader,
            @Valid @RequestBody UpdatePostRequestDTO request) {
        Post post = postService.atualizar(groupId, postId, resolveRequesterEmail(requesterEmailHeader), request);
        return ResponseEntity.ok(post);
    }

    /**
     * DELETE /api/research-groups/{groupId}/posts/{postId} - Remove um post do grupo (Apenas Líder).
     *
     * @param groupId o ID do grupo de pesquisa
     * @param postId o ID do post
     * @param requesterEmailHeader o e-mail do líder solicitante no Header {@code X-Requester-Email} (opcional se autenticado)
     * @return {@link ResponseEntity} sem conteúdo (204 No Content)
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable("groupId") String groupId,
            @PathVariable("postId") Long postId,
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmailHeader) {
        postService.excluir(groupId, postId, resolveRequesterEmail(requesterEmailHeader));
        return ResponseEntity.noContent().build();
    }
}
