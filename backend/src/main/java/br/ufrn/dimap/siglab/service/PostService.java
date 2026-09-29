package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.CreatePostRequestDTO;
import br.ufrn.dimap.siglab.dto.UpdatePostRequestDTO;
import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.model.User_ResearchGroup;
import br.ufrn.dimap.siglab.model.User_ResearchGroupId;
import br.ufrn.dimap.siglab.repository.PostRepository;
import br.ufrn.dimap.siglab.repository.ResearchGroupRepository;
import br.ufrn.dimap.siglab.repository.UserRepository;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final ResearchGroupRepository researchGroupRepository;
    private final UserRepository userRepository;
    private final User_ResearchGroupRepository userResearchGroupRepository;

    public PostService(PostRepository postRepository,
                       ResearchGroupRepository researchGroupRepository,
                       UserRepository userRepository,
                       User_ResearchGroupRepository userResearchGroupRepository) {
        this.postRepository = postRepository;
        this.researchGroupRepository = researchGroupRepository;
        this.userRepository = userRepository;
        this.userResearchGroupRepository = userResearchGroupRepository;
    }

    /**
     * Lista os posts de um grupo de pesquisa do mais recente para o mais antigo.
     * Disponível para qualquer usuário cadastrado no sistema.
     *
     * @param idGrupo o identificador do grupo de pesquisa
     * @param requesterEmail o e-mail do solicitante
     * @return a lista de posts do grupo
     */
    @Transactional(readOnly = true)
    public List<Post> listar(String idGrupo, String requesterEmail) {
        validarGrupo(idGrupo);
        validarLeitor(requesterEmail);
        return postRepository.findByGroupIdOrderByCreatedAtDesc(idGrupo);
    }

    /**
     * Busca um post pertencente ao grupo de pesquisa informado.
     *
     * @param idGrupo o identificador do grupo de pesquisa
     * @param postId o identificador do post
     * @param requesterEmail o e-mail do solicitante
     * @return o post encontrado
     */
    @Transactional(readOnly = true)
    public Post buscar(String idGrupo, Long postId, String requesterEmail) {
        validarGrupo(idGrupo);
        validarLeitor(requesterEmail);
        return buscarPostDoGrupo(postId, idGrupo);
    }

    /**
     * Publica um novo post no grupo de pesquisa (Apenas Líder).
     * O publicador é sempre o solicitante identificado pelo header de requisição.
     *
     * @param idGrupo o identificador do grupo de pesquisa
     * @param requesterEmail o e-mail do líder solicitante
     * @param request o corpo da requisição com os dados do post
     * @return o post publicado
     */
    @Transactional
    public Post criar(String idGrupo, String requesterEmail, CreatePostRequestDTO request) {
        validarGrupo(idGrupo);
        validarLider(idGrupo, requesterEmail);

        User publisher = userRepository.findByEmail(requesterEmail.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário publicador não encontrado com o e-mail: " + requesterEmail));

        Post post = Post.builder()
                .groupId(idGrupo)
                .publisherUserId(publisher.getId())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .content(request.getContent())
                .tags(request.getTags())
                .filePath(request.getFilePath())
                .createdAt(LocalDateTime.now())
                .build();

        return postRepository.save(post);
    }

    /**
     * Atualiza um post existente do grupo de pesquisa (Apenas Líder).
     *
     * @param idGrupo o identificador do grupo de pesquisa
     * @param postId o identificador do post
     * @param requesterEmail o e-mail do líder solicitante
     * @param request o corpo da requisição com os novos dados do post
     * @return o post atualizado
     */
    @Transactional
    public Post atualizar(String idGrupo, Long postId, String requesterEmail, UpdatePostRequestDTO request) {
        validarGrupo(idGrupo);
        validarLider(idGrupo, requesterEmail);

        Post post = buscarPostDoGrupo(postId, idGrupo);
        post.setTitle(request.getTitle().trim());
        post.setDescription(request.getDescription());
        post.setContent(request.getContent());
        post.setTags(request.getTags());
        post.setFilePath(request.getFilePath());

        return postRepository.save(post);
    }

    /**
     * Exclui um post do grupo de pesquisa (Apenas Líder).
     *
     * @param idGrupo o identificador do grupo de pesquisa
     * @param postId o identificador do post
     * @param requesterEmail o e-mail do líder solicitante
     */
    @Transactional
    public void excluir(String idGrupo, Long postId, String requesterEmail) {
        validarGrupo(idGrupo);
        validarLider(idGrupo, requesterEmail);

        postRepository.delete(buscarPostDoGrupo(postId, idGrupo));
    }

    private void validarGrupo(String idGrupo) {
        if (!researchGroupRepository.existsById(idGrupo)) {
            throw new ResourceNotFoundException(
                    "Grupo de pesquisa não encontrado com o ID: " + idGrupo);
        }
    }

    /**
     * Garante que o solicitante esteja logado, ou seja, possua um cadastro válido.
     *
     * @param requesterEmail o e-mail do solicitante
     */
    private void validarLeitor(String requesterEmail) {
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new SecurityException(
                    "Acesso negado: é necessário estar logado para acessar os posts do grupo.");
        }
        if (!userRepository.existsByEmail(requesterEmail.trim())) {
            throw new SecurityException(
                    "Acesso negado: o solicitante não possui um cadastro válido no sistema.");
        }
    }

    /**
     * Garante que o solicitante seja líder do grupo de pesquisa informado.
     *
     * @param idGrupo o identificador do grupo de pesquisa
     * @param requesterEmail o e-mail do líder solicitante
     */
    private void validarLider(String idGrupo, String requesterEmail) {
        validarLeitor(requesterEmail);

        User_ResearchGroupId requesterId = new User_ResearchGroupId(requesterEmail.trim(), idGrupo);
        boolean isLeader = userResearchGroupRepository.findById(requesterId)
                .map(User_ResearchGroup::isLeader)
                .orElse(false);

        if (!isLeader) {
            throw new SecurityException(
                    "Acesso negado: apenas líderes do grupo podem gerenciar posts.");
        }
    }

    private Post buscarPostDoGrupo(Long postId, String idGrupo) {
        return postRepository.findByIdAndGroupId(postId, idGrupo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Post não encontrado no grupo informado com o ID: " + postId));
    }
}
