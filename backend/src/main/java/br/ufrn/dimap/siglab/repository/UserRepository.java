package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    void deleteByEmail(String email);

    /**
     * Busca todos os usuários que possuem o ID do grupo informado em seu array {@code groups}.
     *
     * @param groupId o ID do grupo de pesquisa
     * @return lista de usuários associados ao grupo
     */
    @Query(value = "SELECT * FROM users WHERE :groupId = ANY(groups)", nativeQuery = true)
    List<User> findByGroupIdInArray(@Param("groupId") String groupId);
}
