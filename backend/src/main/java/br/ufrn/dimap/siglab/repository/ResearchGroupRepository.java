package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.model.ResearchGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResearchGroupRepository extends JpaRepository<ResearchGroup, String> {
}
