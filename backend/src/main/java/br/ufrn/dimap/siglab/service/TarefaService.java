package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.CreateTarefaRequestDTO;
import br.ufrn.dimap.siglab.dto.MoveTarefaRequestDTO;
import br.ufrn.dimap.siglab.dto.UpdateTarefaRequestDTO;
import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.model.Coluna;
import br.ufrn.dimap.siglab.model.Tarefa;
import br.ufrn.dimap.siglab.repository.ColunaRepository;
import br.ufrn.dimap.siglab.repository.ResearchGroupRepository;
import br.ufrn.dimap.siglab.repository.TarefaRepository;
import br.ufrn.dimap.siglab.repository.UserRepository;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final ColunaRepository colunaRepository;
    private final ResearchGroupRepository researchGroupRepository;
    private final UserRepository userRepository;
    private final User_ResearchGroupRepository userResearchGroupRepository;

    public TarefaService(TarefaRepository tarefaRepository,
                         ColunaRepository colunaRepository,
                         ResearchGroupRepository researchGroupRepository,
                         UserRepository userRepository,
                         User_ResearchGroupRepository userResearchGroupRepository) {
        this.tarefaRepository = tarefaRepository;
        this.colunaRepository = colunaRepository;
        this.researchGroupRepository = researchGroupRepository;
        this.userRepository = userRepository;
        this.userResearchGroupRepository = userResearchGroupRepository;
    }

    @Transactional
    public Tarefa criar(String idGrupo, Long creatorId, CreateTarefaRequestDTO request) {
        validarGrupo(idGrupo);
        buscarColunaDoGrupo(request.getIdColuna(), idGrupo);
        validarMembro(creatorId, idGrupo, "criador");
        validarResponsavel(request.getIdAtribuidoA(), idGrupo);

        Tarefa tarefa = Tarefa.builder()
                .idGrupo(idGrupo)
                .idCriadoPor(creatorId)
                .idAtribuidoA(request.getIdAtribuidoA())
                .idColuna(request.getIdColuna())
                .titulo(request.getTitulo().trim())
                .descricao(request.getDescricao())
                .tags(request.getTags())
                .build();

        return tarefaRepository.save(tarefa);
    }

    @Transactional
    public Tarefa atualizar(String idGrupo, Long tarefaId, UpdateTarefaRequestDTO request) {
        Tarefa tarefa = buscarTarefaDoGrupo(tarefaId, idGrupo);
        validarResponsavel(request.getIdAtribuidoA(), idGrupo);

        tarefa.setTitulo(request.getTitulo().trim());
        tarefa.setDescricao(request.getDescricao());
        tarefa.setTags(request.getTags());
        tarefa.setIdAtribuidoA(request.getIdAtribuidoA());
        return tarefaRepository.save(tarefa);
    }

    @Transactional
    public Tarefa mover(String idGrupo, Long tarefaId, MoveTarefaRequestDTO request) {
        Tarefa tarefa = buscarTarefaDoGrupo(tarefaId, idGrupo);
        Coluna destino = buscarColunaDoGrupo(request.getIdColunaDestino(), idGrupo);
        tarefa.setIdColuna(destino.getId());
        return tarefaRepository.save(tarefa);
    }

    @Transactional
    public void excluir(String idGrupo, Long tarefaId) {
        tarefaRepository.delete(buscarTarefaDoGrupo(tarefaId, idGrupo));
    }

    private void validarGrupo(String idGrupo) {
        if (!researchGroupRepository.existsById(idGrupo)) {
            throw new ResourceNotFoundException(
                    "Grupo de pesquisa nao encontrado com o ID: " + idGrupo);
        }
    }

    private Tarefa buscarTarefaDoGrupo(Long tarefaId, String idGrupo) {
        return tarefaRepository.findByIdAndIdGrupo(tarefaId, idGrupo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tarefa nao encontrada no grupo informado com o ID: " + tarefaId));
    }

    private Coluna buscarColunaDoGrupo(Long colunaId, String idGrupo) {
        return colunaRepository.findByIdAndIdGrupoPesquisa(colunaId, idGrupo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Coluna nao encontrada no grupo informado com o ID: " + colunaId));
    }

    private void validarResponsavel(Long usuarioId, String idGrupo) {
        if (usuarioId != null) {
            validarMembro(usuarioId, idGrupo, "usuario atribuido");
        }
    }

    private void validarMembro(Long usuarioId, String idGrupo, String papel) {
        if (!userRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException(
                    "Usuario " + papel + " nao encontrado com o ID: " + usuarioId);
        }
        if (!userResearchGroupRepository.existsMemberByUserIdAndGroupId(usuarioId, idGrupo)) {
            throw new IllegalArgumentException(
                    "O " + papel + " deve ser membro do grupo de pesquisa.");
        }
    }
}
