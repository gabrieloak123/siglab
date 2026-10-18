package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Construtor para injeção de dependências do repositório de usuários e codificador de senhas.
     *
     * @param userRepository o repositório {@link UserRepository}
     * @param passwordEncoder o codificador de senhas {@link PasswordEncoder}
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, InterestService interests) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.interests = interests;
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
            user.setAuthorId(user.getAuthorId().trim());
            if (userRepository.existsByAuthorId(user.getAuthorId())) throw new IllegalArgumentException("author_id já cadastrado.");
        } else {
            user.setAuthorId(null);
        }
        user.setInterest_areas(interests.three(user.getInterest_areas()));
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String trimmedEmail = user.getEmail().trim();
            user.setEmail(trimmedEmail);
            if (userRepository.existsByEmail(trimmedEmail)) {
                throw new IllegalArgumentException("Já existe um usuário cadastrado com o e-mail: " + trimmedEmail);
            }
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            if (!isAlreadyBcryptEncoded(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }
        User saved = userRepository.save(user);
        interests.index(saved);
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
    @Transactional
    public User update(Long id, User userDetails) {
        if (userDetails.getName() == null || userDetails.getName().isBlank()
                || userDetails.getEmail() == null || userDetails.getEmail().isBlank())
            throw new IllegalArgumentException("Nome e e-mail são obrigatórios.");
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(userDetails.getName());
                    user.setEmail(userDetails.getEmail());
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
                        if (!isAlreadyBcryptEncoded(userDetails.getPassword())) {
                            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                        } else {
                            user.setPassword(userDetails.getPassword());
                        }
                    }
                    String[] requestedAreas = userDetails.getInterest_areas();
                    boolean scholarChanged = false;
                    if (userDetails.getAuthorId() != null && !userDetails.getAuthorId().isBlank()) {
                        String authorId = userDetails.getAuthorId().trim();
                        if (!authorId.equals(user.getAuthorId()) && userRepository.existsByAuthorId(authorId))
                            throw new IllegalArgumentException("author_id já cadastrado.");
                        scholarChanged = !authorId.equals(user.getAuthorId());
                        user.setAuthorId(authorId);
                    }
                    user.setInterest_areas(scholarChanged
                            ? interests.withScholar(user.getAuthorId(), requestedAreas)
                            : interests.three(requestedAreas));
                    User saved = userRepository.save(user);
                    interests.index(saved);
                    return saved;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
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
            throw new SecurityException("A confirmação da senha é obrigatória para excluir a conta de usuário.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new SecurityException("Senha incorreta. Não foi possível excluir a conta.");
        }

        userRepository.deleteById(id);
    }

    /**
     * Verifica se uma hash de senha já está formatada no padrão BCrypt ($2a$, $2b$ ou $2y$).
     *
     * @param password a string de senha
     * @return {@code true} se já for uma hash BCrypt válida
     */
    private boolean isAlreadyBcryptEncoded(String password) {
        if (password == null) return false;
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
