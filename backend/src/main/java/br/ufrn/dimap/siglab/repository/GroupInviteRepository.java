package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.model.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInvite, String> {
    Optional<GroupInvite> findByTokenAndUsedFalse(String token);

    /**
     * Remove todos os convites associados a um determinado grupo de pesquisa.
     *
     * @param groupId o ID do grupo de pesquisa
     */
    void deleteByGroupId(String groupId);
}
