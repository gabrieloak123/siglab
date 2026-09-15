package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável por conter as regras de negócio relacionadas a Usuários.
 *
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    /**
     * Construtor para injeção de dependências do repositório de usuários.
     *
     * @param userRepository o repositório {@link UserRepository}
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retorna a lista completa de todos os usuários cadastrados no sistema.
     *
     * @return lista de {@link User}
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Busca um usuário pelo seu ID numérico.
     *
     * @param id o ID do usuário
     * @return {@link Optional} contendo o usuário se encontrado
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Busca um usuário pelo e-mail.
     *
     * @param email o e-mail do usuário
     * @return {@link Optional} contendo o usuário se encontrado
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Salva ou cria um novo usuário no banco de dados.
     *
     * @param user a entidade usuário a ser salva
     * @return o usuário salvo com o ID preenchido
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Atualiza os dados de um usuário existente.
     *
     * @param id o ID do usuário a atualizar
     * @param userDetails os novos dados do usuário
     * @return o usuário atualizado
     * @throws RuntimeException se o usuário não for encontrado
     */
    public User update(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(userDetails.getName());
                    user.setEmail(userDetails.getEmail());
                    user.setPassword(userDetails.getPassword());
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
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuário não encontrado com o ID: " + id);
        }
        userRepository.deleteById(id);
    }
}
