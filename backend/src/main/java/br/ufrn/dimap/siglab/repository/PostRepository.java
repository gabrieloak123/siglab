package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Lista os posts de um grupo, do mais recente para o mais antigo.
     *
     * @param groupId o identificador do grupo de pesquisa
     * @return a lista de posts do grupo
     */
    List<Post> findByGroupIdOrderByCreatedAtDesc(String groupId);

    /**
     * Busca um post garantindo que ele pertença ao grupo informado.
     *
     * @param id o identificador do post
     * @param groupId o identificador do grupo de pesquisa
     * @return o post, se existir e pertencer ao grupo
     */
    Optional<Post> findByIdAndGroupId(Long id, String groupId);
}
