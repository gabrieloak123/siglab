package br.ufrn.dimap.siglab.service;

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

    /**
     * Construtor para injeção de dependências do repositório de usuários e codificador de senhas.
     *
     * @param userRepository o repositório {@link UserRepository}
     * @param passwordEncoder o codificador de senhas {@link PasswordEncoder}
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
     */
    @Transactional
    public User save(User user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            if (!isAlreadyBcryptEncoded(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }
        return userRepository.save(user);
    }

    /**
     * Atualiza os dados de um usuário existente, criptografando a nova senha caso alterada.
     *
     * @param id o ID do usuário a atualizar
     * @param userDetails os novos dados do usuário
     * @return o usuário atualizado
     * @throws RuntimeException se o usuário não for encontrado
     */
    @Transactional
    public User update(Long id, User userDetails) {
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
                    user.setInterest_areas(userDetails.getInterest_areas());
                    user.setGroups(userDetails.getGroups());
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com o ID: " + id));
    }

    /**
     * Exclui um usuário pelo seu ID.
     *
     * @param id o ID do usuário a ser deletado
     * @throws RuntimeException se o usuário não for encontrado
     */
    @Transactional
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuário não encontrado com o ID: " + id);
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
