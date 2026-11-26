package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.controller.GroupDashboardController;
import br.ufrn.dimap.siglab.model.Coluna;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.Tarefa;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.ColunaRepository;
import br.ufrn.dimap.siglab.repository.PostRepository;
import br.ufrn.dimap.siglab.repository.TarefaRepository;
import br.ufrn.dimap.siglab.repository.UserRepository;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupDashboardApiTest {

    @Test
    void ranksCurrentMembersByCompletedAssignedTasks() throws Exception {
        var users = mock(UserRepository.class);
        var memberships = mock(User_ResearchGroupRepository.class);
        var columns = mock(ColunaRepository.class);
        var tasks = mock(TarefaRepository.class);
        var posts = mock(PostRepository.class);
        var ana = User.builder().id(1L).name("Ana").build();
        var bia = User.builder().id(2L).name("Bia").build();
        when(users.findById(1L)).thenReturn(Optional.of(ana));
        when(memberships.existsMemberByUserIdAndGroupId(1L, "lab")).thenReturn(true);
        when(memberships.findUsersByGroupId("lab")).thenReturn(List.of(ana, bia));
        when(columns.findByIdGrupoPesquisaOrderByOrdemAsc("lab")).thenReturn(List.of(
                Coluna.builder().id(10L).nome("A fazer").build(),
                Coluna.builder().id(20L).nome("Concluído").build()));
        when(tasks.findByIdGrupo("lab")).thenReturn(List.of(
                task(20L, 1L), task(20L, 2L), task(20L, 2L),
                task(10L, 1L), task(20L, 99L), task(20L, null)));

        var dashboard = new SiglabDashboardConfiguration()
                .siglabDashboard(memberships, columns, tasks, posts);
        var controller = new GroupDashboardController(new SessionUser(users), memberships, dashboard);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/api/research-groups/lab/dashboard").sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics[3].id").value("members-by-completed-tasks"))
                .andExpect(jsonPath("$.metrics[3].items.length()").value(2))
                .andExpect(jsonPath("$.metrics[3].items[0].label").value("Bia"))
                .andExpect(jsonPath("$.metrics[3].items[0].value").value(2))
                .andExpect(jsonPath("$.metrics[3].items[1].label").value("Ana"))
                .andExpect(jsonPath("$.metrics[3].items[1].value").value(1));
    }

    @Test
    void reportsPostCountsByTypeForTheRequestedGroup() throws Exception {
        var users = mock(UserRepository.class);
        var memberships = mock(User_ResearchGroupRepository.class);
        var columns = mock(ColunaRepository.class);
        var tasks = mock(TarefaRepository.class);
        var posts = mock(PostRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(memberships.existsMemberByUserIdAndGroupId(1L, "lab")).thenReturn(true);
        when(posts.findByGroupIdOrderByCreatedAtDesc("lab")).thenReturn(List.of(
                Post.builder().postType("ARTIGO").build(),
                Post.builder().postType("OUTROS").build(),
                Post.builder().postType("ARTIGO").build()));
        var dashboard = new SiglabDashboardConfiguration()
                .siglabDashboard(memberships, columns, tasks, posts);
        var mvc = MockMvcBuilders.standaloneSetup(
                new GroupDashboardController(new SessionUser(users), memberships, dashboard)).build();

        mvc.perform(get("/api/research-groups/lab/dashboard").sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics[4].id").value("posts-by-type"))
                .andExpect(jsonPath("$.metrics[4].items[0].label").value("Artigo"))
                .andExpect(jsonPath("$.metrics[4].items[0].value").value(2))
                .andExpect(jsonPath("$.metrics[4].items[1].label").value("Outros"))
                .andExpect(jsonPath("$.metrics[4].items[1].value").value(1));
    }

    @Test
    void countsEachTagOncePerPost() throws Exception {
        var users = mock(UserRepository.class);
        var memberships = mock(User_ResearchGroupRepository.class);
        var columns = mock(ColunaRepository.class);
        var tasks = mock(TarefaRepository.class);
        var posts = mock(PostRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(memberships.existsMemberByUserIdAndGroupId(1L, "lab")).thenReturn(true);
        when(posts.findByGroupIdOrderByCreatedAtDesc("lab")).thenReturn(List.of(
                Post.builder().tags(new String[]{" Java ", "JAVA", "Spring", ""}).build(),
                Post.builder().tags(new String[]{"java"}).build(),
                Post.builder().build()));
        var dashboard = new SiglabDashboardConfiguration()
                .siglabDashboard(memberships, columns, tasks, posts);
        var mvc = MockMvcBuilders.standaloneSetup(
                new GroupDashboardController(new SessionUser(users), memberships, dashboard)).build();

        mvc.perform(get("/api/research-groups/lab/dashboard").sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics[5].id").value("post-tags-cloud"))
                .andExpect(jsonPath("$.metrics[5].items.length()").value(2))
                .andExpect(jsonPath("$.metrics[5].items[0].label").value("java"))
                .andExpect(jsonPath("$.metrics[5].items[0].value").value(2))
                .andExpect(jsonPath("$.metrics[5].items[1].label").value("spring"))
                .andExpect(jsonPath("$.metrics[5].items[1].value").value(1));
    }

    @Test
    void countsEachInterestOncePerMember() throws Exception {
        var users = mock(UserRepository.class);
        var memberships = mock(User_ResearchGroupRepository.class);
        var columns = mock(ColunaRepository.class);
        var tasks = mock(TarefaRepository.class);
        var posts = mock(PostRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(memberships.existsMemberByUserIdAndGroupId(1L, "lab")).thenReturn(true);
        when(memberships.findUsersByGroupId("lab")).thenReturn(List.of(
                User.builder().interest_areas(new String[]{" Redes ", "REDES", "Sistemas distribuídos"}).build(),
                User.builder().interest_areas(new String[]{"redes", ""}).build(),
                User.builder().build()));
        var dashboard = new SiglabDashboardConfiguration()
                .siglabDashboard(memberships, columns, tasks, posts);
        var mvc = MockMvcBuilders.standaloneSetup(
                new GroupDashboardController(new SessionUser(users), memberships, dashboard)).build();

        mvc.perform(get("/api/research-groups/lab/dashboard").sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics[6].id").value("member-interests-cloud"))
                .andExpect(jsonPath("$.metrics[6].items.length()").value(2))
                .andExpect(jsonPath("$.metrics[6].items[0].label").value("redes"))
                .andExpect(jsonPath("$.metrics[6].items[0].value").value(2))
                .andExpect(jsonPath("$.metrics[6].items[1].label").value("sistemas distribuídos"))
                .andExpect(jsonPath("$.metrics[6].items[1].value").value(1));
    }

    private Tarefa task(Long columnId, Long assigneeId) {
        return Tarefa.builder().idColuna(columnId).idAtribuidoA(assigneeId).build();
    }
}
