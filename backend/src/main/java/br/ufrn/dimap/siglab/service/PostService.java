package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.CreatePostRequestDTO;
import br.ufrn.dimap.siglab.dto.UpdatePostRequestDTO;
import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.exception.AccessDeniedException;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.model.User_ResearchGroup;
import br.ufrn.dimap.siglab.model.User_ResearchGroupId;
import br.ufrn.dimap.siglab.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class PostService {
    private final PostRepository posts;
    private final ResearchGroupRepository groups;
    private final UserRepository users;
    private final User_ResearchGroupRepository memberships;
    private final IndexingQueue indexing;
    private final PostLikeRepository likes;
    public PostService(PostRepository posts, ResearchGroupRepository groups, UserRepository users,
                       User_ResearchGroupRepository memberships, IndexingQueue indexing, PostLikeRepository likes) {
        this.posts=posts; this.groups=groups; this.users=users; this.memberships=memberships;
        this.indexing=indexing; this.likes=likes;
    }
    @Transactional(readOnly=true)
    public List<Post> listar(String groupId, String email) {
        checkGroup(groupId); checkReader(email);
        return posts.findByGroupIdOrderByCreatedAtDesc(groupId);
    }
    @Transactional(readOnly=true)
    public Post buscar(String groupId, Long id, String email) {
        checkGroup(groupId); checkReader(email);
        return post(groupId,id);
    }
    @Transactional
    public Post criar(String groupId, String email, CreatePostRequestDTO request) {
        User publisher = checkLeader(groupId,email);
        String[] terms=terms(request.getPostType(), request.getKeywords());
        Post saved=posts.save(Post.builder().groupId(groupId).publisherUserId(publisher.getId())
                .title(request.getTitle().trim()).description(request.getDescription()).content(request.getContent())
                .tags(request.getTags()).filePath(request.getFilePath()).postType(request.getPostType().toUpperCase())
                .keywords(terms).createdAt(LocalDateTime.now()).build());
        indexing.enqueue("POST", saved.getId().toString());
        return saved;
    }
    @Transactional
    public Post atualizar(String groupId, Long id, String email, UpdatePostRequestDTO request) {
        checkLeader(groupId,email);
        Post post=post(groupId,id);
        String[] terms=terms(request.getPostType(),request.getKeywords());
        post.setTitle(request.getTitle().trim()); post.setDescription(request.getDescription());
        post.setContent(request.getContent()); post.setTags(request.getTags()); post.setFilePath(request.getFilePath());
        post.setPostType(request.getPostType().toUpperCase()); post.setKeywords(terms);
        Post saved=posts.save(post);
        indexing.enqueue("POST", saved.getId().toString());
        return saved;
    }
    @Transactional
    public void excluir(String groupId, Long id, String email) {
        checkLeader(groupId,email);
        Post target=post(groupId,id);
        List<Long> affectedUsers=likes.usersForPost(id);
        posts.delete(target);
        posts.flush();
        affectedUsers.forEach(likes::refreshVector);
    }
    private String[] terms(String type, String[] supplied) {
        if ("ARTIGO".equalsIgnoreCase(type)) {
            if (supplied == null || supplied.length == 0) throw new IllegalArgumentException("Artigo precisa de palavras-chave.");
            String[] clean=Arrays.stream(supplied).filter(s -> s != null && !s.isBlank()).map(String::trim)
                    .collect(java.util.stream.Collectors.toMap(s -> s.toLowerCase(java.util.Locale.ROOT), s -> s, (a,b) -> a, java.util.LinkedHashMap::new))
                    .values().toArray(String[]::new);
            if (clean.length == 0) throw new IllegalArgumentException("Artigo precisa de palavras-chave.");
            if (clean.length > 10 || Arrays.stream(clean).anyMatch(s -> s.length() > 100))
                throw new IllegalArgumentException("Informe no máximo 10 palavras-chave de até 100 caracteres.");
            return clean;
        }
        if ("OUTROS".equalsIgnoreCase(type)) return new String[0];
        throw new IllegalArgumentException("Tipo de post deve ser ARTIGO ou OUTROS.");
    }
    private void checkGroup(String groupId) {
        if (!groups.existsById(groupId)) throw new ResourceNotFoundException("Grupo não encontrado: " + groupId);
    }
    private void checkReader(String email) {
        if (email == null || !users.existsByEmail(email)) throw new AccessDeniedException("Entre para acessar os posts.");
    }
    private User checkLeader(String groupId, String email) {
        checkGroup(groupId); checkReader(email);
        boolean leader=memberships.findById(new User_ResearchGroupId(email,groupId)).map(User_ResearchGroup::isLeader).orElse(false);
        if (!leader) throw new AccessDeniedException("Apenas líderes podem gerenciar posts.");
        return users.findByEmail(email).orElseThrow();
    }
    private Post post(String groupId,Long id) {
        return posts.findByIdAndGroupId(id,groupId).orElseThrow(() -> new ResourceNotFoundException("Post não encontrado no grupo."));
    }
}
