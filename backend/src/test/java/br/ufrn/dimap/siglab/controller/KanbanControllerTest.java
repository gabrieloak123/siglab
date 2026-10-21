package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.CreateTarefaRequestDTO;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import br.ufrn.dimap.siglab.service.EstadoService;
import br.ufrn.dimap.siglab.service.SessionUser;
import br.ufrn.dimap.siglab.service.TarefaService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class KanbanControllerTest {
    private final EstadoService states = mock(EstadoService.class);
    private final TarefaService tasks = mock(TarefaService.class);
    private final SessionUser session = mock(SessionUser.class);
    private final User_ResearchGroupRepository memberships = mock(User_ResearchGroupRepository.class);
    private final KanbanController controller = new KanbanController(states, tasks, session, memberships);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test void blocksBoardAccessForNonMembers() {
        when(session.require(request)).thenReturn(User.builder().id(12L).build());
        assertThrows(SecurityException.class, () -> controller.buscarEstado("lab", request));
        verifyNoInteractions(states);
    }

    @Test void usesTheAuthenticatedMemberAsTaskCreator() {
        when(session.require(request)).thenReturn(User.builder().id(12L).build());
        when(memberships.existsMemberByUserIdAndGroupId(12L, "lab")).thenReturn(true);
        var task = new CreateTarefaRequestDTO();
        controller.criarTarefa("lab", task, request);
        verify(tasks).criar("lab", 12L, task);
    }
}
