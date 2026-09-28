package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    List<Tarefa> findByIdGrupo(String idGrupo);
}
