package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.model.ResearchGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ResearchGroupRepository extends JpaRepository<ResearchGroup, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM ResearchGroup g WHERE g.group_id = :id")
    Optional<ResearchGroup> findForUpdate(@Param("id") String id);
}
