package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.model.Coluna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColunaRepository extends JpaRepository<Coluna, Long> {
    List<Coluna> findByIdGrupoPesquisaOrderByOrdemAsc(String idGrupoPesquisa);

    Optional<Coluna> findByIdAndIdGrupoPesquisa(Long id, String idGrupoPesquisa);
}
