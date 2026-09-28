package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.model.Coluna;
import br.ufrn.dimap.siglab.model.Estado;
import br.ufrn.dimap.siglab.model.Tarefa;
import br.ufrn.dimap.siglab.repository.ColunaRepository;
import br.ufrn.dimap.siglab.repository.ResearchGroupRepository;
import br.ufrn.dimap.siglab.repository.TarefaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EstadoService {

    private final ResearchGroupRepository researchGroupRepository;
    private final ColunaRepository colunaRepository;
    private final TarefaRepository tarefaRepository;

    public EstadoService(ResearchGroupRepository researchGroupRepository,
                         ColunaRepository colunaRepository,
                         TarefaRepository tarefaRepository) {
        this.researchGroupRepository = researchGroupRepository;
        this.colunaRepository = colunaRepository;
        this.tarefaRepository = tarefaRepository;
    }

    @Transactional
    public Estado buscarPorGrupo(String idGrupo) {
        if (!researchGroupRepository.existsById(idGrupo)) {
            throw new ResourceNotFoundException(
                    "Grupo de pesquisa nao encontrado com o ID: " + idGrupo);
        }

        List<Coluna> colunas = colunaRepository
                .findByIdGrupoPesquisaOrderByOrdemAsc(idGrupo);
        if (colunas.isEmpty()) {
            colunas = criarColunasPadrao(idGrupo);
        }

        Map<Long, List<Tarefa>> tarefasPorColuna = tarefaRepository.findByIdGrupo(idGrupo)
                .stream()
                .collect(Collectors.groupingBy(Tarefa::getIdColuna));

        colunas.forEach(coluna -> coluna.setTarefas(new ArrayList<>(
                tarefasPorColuna.getOrDefault(coluna.getId(), List.of()))));

        return Estado.builder()
                .id(idGrupo)
                .idGrupoPesquisa(idGrupo)
                .colunas(colunas)
                .build();
    }

    private List<Coluna> criarColunasPadrao(String idGrupo) {
        List<Coluna> colunasPadrao = List.of(
                novaColuna("A fazer", 1, idGrupo),
                novaColuna("Em andamento", 2, idGrupo),
                novaColuna("Concluído", 3, idGrupo)
        );
        return colunaRepository.saveAll(colunasPadrao);
    }

    private Coluna novaColuna(String nome, int ordem, String idGrupo) {
        return Coluna.builder()
                .nome(nome)
                .ordem(ordem)
                .idGrupoPesquisa(idGrupo)
                .build();
    }
}
