package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.CreateTarefaRequestDTO;
import br.ufrn.dimap.siglab.dto.MoveTarefaRequestDTO;
import br.ufrn.dimap.siglab.dto.UpdateTarefaRequestDTO;
import br.ufrn.dimap.siglab.model.Estado;
import br.ufrn.dimap.siglab.model.Tarefa;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.service.SessionUser;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import jakarta.servlet.http.HttpServletRequest;
import br.ufrn.dimap.siglab.service.EstadoService;
import br.ufrn.dimap.siglab.service.TarefaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/research-groups/{groupId}")
@CrossOrigin(origins = "*")
public class KanbanController {

    private final EstadoService estadoService;
    private final TarefaService tarefaService;
    private final SessionUser sessionUser;
    private final User_ResearchGroupRepository memberships;

    public KanbanController(EstadoService estadoService, TarefaService tarefaService, SessionUser sessionUser,
                            User_ResearchGroupRepository memberships) {
        this.estadoService = estadoService;
        this.tarefaService = tarefaService;
        this.sessionUser = sessionUser;
        this.memberships = memberships;
    }

    private User requireMember(String groupId, HttpServletRequest request) {
        User user = sessionUser.require(request);
        if (!memberships.existsMemberByUserIdAndGroupId(user.getId(), groupId))
            throw new SecurityException("Apenas membros podem acessar o quadro do grupo.");
        return user;
    }

    @GetMapping("/board")
    public ResponseEntity<Estado> buscarEstado(@PathVariable String groupId, HttpServletRequest request) {
        requireMember(groupId, request);
        return ResponseEntity.ok(estadoService.buscarPorGrupo(groupId));
    }

    @PostMapping("/tasks")
    public ResponseEntity<Tarefa> criarTarefa(
            @PathVariable String groupId,
            @Valid @RequestBody CreateTarefaRequestDTO request, HttpServletRequest servletRequest) {
        User user = requireMember(groupId, servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tarefaService.criar(groupId, user.getId(), request));
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<Tarefa> atualizarTarefa(
            @PathVariable String groupId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTarefaRequestDTO request, HttpServletRequest servletRequest) {
        requireMember(groupId, servletRequest);
        return ResponseEntity.ok(tarefaService.atualizar(groupId, taskId, request));
    }

    @PatchMapping("/tasks/{taskId}/column")
    public ResponseEntity<Tarefa> moverTarefa(
            @PathVariable String groupId,
            @PathVariable Long taskId,
            @Valid @RequestBody MoveTarefaRequestDTO request, HttpServletRequest servletRequest) {
        requireMember(groupId, servletRequest);
        return ResponseEntity.ok(tarefaService.mover(groupId, taskId, request));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> excluirTarefa(
            @PathVariable String groupId,
            @PathVariable Long taskId, HttpServletRequest request) {
        requireMember(groupId, request);
        tarefaService.excluir(groupId, taskId);
        return ResponseEntity.noContent().build();
    }
}
