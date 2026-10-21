package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.exception.ConflictException;
import br.ufrn.dimap.siglab.exception.AccessDeniedException;
import br.ufrn.dimap.siglab.exception.GroupMustHaveLeaderException;
import br.ufrn.dimap.siglab.dto.UpdateUserRequest;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import br.ufrn.dimap.siglab.repository.ResearchGroupRepository;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import br.ufrn.dimap.siglab.model.User_ResearchGroup;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável por conter as regras de negócio relacionadas a Usuários.
 *
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InterestService interests;
    private final IndexingQueue indexing;
    private final TransactionTemplate transaction;
    private final ResearchGroupRepository groups;
    private final User_ResearchGroupRepository memberships;

    /**
     * Construtor para injeção de dependências do repositório de usuários e codificador de senhas.
     *
     * @param userRepository o repositório {@link UserRepository}
     * @param passwordEncoder o codificador de senhas {@link PasswordEncoder}
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, InterestService interests, IndexingQueue indexing,
                       TransactionTemplate transaction, ResearchGroupRepository groups, User_ResearchGroupRepository memberships) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.interests = interests;
        this.indexing = indexing;
        this.transaction = transaction;
        this.groups = groups;
        this.memberships = memberships;
    }

    /**
     * Retorna a lista completa de todos os usuários cadastrados no sistema.
     *
     * @return lista de {@link User}
     */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Busca um usuário pelo seu ID numérico.
     *
     * @param id o ID do usuário
     * @return {@link Optional} contendo o usuário se encontrado
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Busca um usuário pelo e-mail.
     *
     * @param email o e-mail do usuário
     * @return {@link Optional} contendo o usuário se encontrado
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Salva ou cria um novo usuário no banco de dados com senha criptografada via BCrypt.
     *
     * @param user a entidade usuário a ser salva
     * @return o usuário salvo com o ID preenchido e senha criptografada
     * @throws IllegalArgumentException se já existir um usuário cadastrado com o mesmo e-mail
     */
    @Transactional
    public User save(User user) {
        if (user.getAuthorId() != null && !user.getAuthorId().isBlank()) {
            if (!"ORCID".equals(user.getVerificationLevel()))
                throw new AccessDeniedException("Vincule o ORCID antes de informar o author_id.");
            user.setAuthorId(user.getAuthorId().trim());
            if (userRepository.existsByAuthorId(user.getAuthorId())) throw new ConflictException("author_id já cadastrado.");
        } else {
            user.setAuthorId(null);
        }
        user.setInterest_areas(interests.three(user.getInterest_areas()));
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String trimmedEmail = user.getEmail().trim();
            user.setEmail(trimmedEmail);
            if (userRepository.existsByEmailIgnoreCase(trimmedEmail)) {
                throw new ConflictException("Já existe um usuário cadastrado com esse e-mail.");
            }
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        indexing.enqueue("USER", saved.getId().toString());
        return saved;
    }

    /**
     * Atualiza os dados de um usuário existente, criptografando a nova senha caso alterada.
     *
     * @param id o ID do usuário a atualizar
     * @param userDetails os novos dados do usuário
     * @return o usuário atualizado
     * @throws ResourceNotFoundException se o usuário não for encontrado
     */
    public User update(Long id, UpdateUserRequest userDetails) {
        if (userDetails.name() == null || userDetails.name().isBlank()
                || userDetails.email() == null || userDetails.email().isBlank())
            throw new IllegalArgumentException("Nome e e-mail são obrigatórios.");
        User current = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
        String authorId = userDetails.authorId() == null || userDetails.authorId().isBlank() ? null : userDetails.authorId().trim();
        boolean scholarChanged = !java.util.Objects.equals(authorId, current.getAuthorId());
        if (scholarChanged && authorId != null && !"ORCID".equals(current.getVerificationLevel()))
            throw new AccessDeniedException("Vincule o ORCID antes de informar o author_id.");
        String[] areas = interests.three(userDetails.interestAreas());
        return transaction.execute(status -> {
                    User user = userRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
                    user.setName(userDetails.name().trim());
                    String email = userDetails.email().trim();
                    userRepository.findByEmailIgnoreCase(email).filter(existing -> !existing.getId().equals(id))
                            .ifPresent(existing -> { throw new ConflictException("E-mail já cadastrado."); });
                    user.setEmail(email);
                    if (userDetails.password() != null && !userDetails.password().isBlank()) {
                        user.setPassword(passwordEncoder.encode(userDetails.password()));
                    }
                    if (authorId != null) {
                        if (!authorId.equals(user.getAuthorId()) && userRepository.existsByAuthorId(authorId))
                            throw new ConflictException("author_id já cadastrado.");
                    }
                    user.setAuthorId(authorId);
                    user.setInterest_areas(areas);
                    User saved = userRepository.save(user);
                    indexing.enqueue("USER", saved.getId().toString());
                    return saved;
                });
    }

    /**
     * Exclui um usuário pelo seu ID mediante confirmação de senha.
     *
     * @param id o ID do usuário a ser deletado
     * @param password a senha de confirmação do usuário
     * @throws ResourceNotFoundException se o usuário não for encontrado
     * @throws SecurityException se a senha fornecida for incorreta ou ausente
     */
    @Transactional
    public void deleteById(Long id, String password) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        if (password == null || password.isBlank()) {
            throw new AccessDeniedException("A confirmação da senha é obrigatória para excluir a conta de usuário.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AccessDeniedException("Senha incorreta. Não foi possível excluir a conta.");
        }

        List<String> ledGroups = memberships.findByUserEmail(user.getEmail()).stream()
                .filter(User_ResearchGroup::isLeader).map(m -> m.getId().getGroupId()).sorted().toList();
        for (String groupId : ledGroups) {
            groups.findForUpdate(groupId);
            long leaders = memberships.findByGroupId(groupId).stream().filter(User_ResearchGroup::isLeader).count();
            if (leaders <= 1) throw new GroupMustHaveLeaderException("Transfira a liderança do grupo " + groupId + " antes de excluir a conta.");
        }

        userRepository.deleteById(id);
    }

}
