package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.model.GroupMetric;
import br.ufrn.dimap.siglab.model.Tarefa;
import br.ufrn.dimap.siglab.repository.ColunaRepository;
import br.ufrn.dimap.siglab.repository.TarefaRepository;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiglabDashboardConfiguration {

    @Bean
    GroupDashboardDefinition<Context> siglabDashboard(
            User_ResearchGroupRepository memberships,
            ColunaRepository columns,
            TarefaRepository tasks
    ) {
        return new GroupDashboardDefinition<>(
                groupId -> loadContext(groupId, memberships, columns, tasks),
                List.of(
                        new GroupMetric<>("members", "Total de membros",
                                context -> new GroupMetric.Total(context.memberCount())),
                        new GroupMetric<>("tasks", "Total de tarefas",
                                context -> new GroupMetric.Total(context.taskCount())),
                        new GroupMetric<>("tasks-by-column", "Tarefas por coluna",
                                context -> new GroupMetric.Breakdown(context.tasksByColumn()))
                )
        );
    }

    private Context loadContext(
            String groupId,
            User_ResearchGroupRepository memberships,
            ColunaRepository columns,
            TarefaRepository tasks
    ) {
        long memberCount = memberships.findByGroupId(groupId).size();
        List<Tarefa> groupTasks = tasks.findByIdGrupo(groupId);
        Map<Long, Long> countsByColumn = groupTasks.stream()
                .collect(Collectors.groupingBy(Tarefa::getIdColuna, Collectors.counting()));

        List<GroupMetric.Item> tasksByColumn =
                columns.findByIdGrupoPesquisaOrderByOrdemAsc(groupId).stream()
                        .map(column -> new GroupMetric.Item(
                                column.getId().toString(),
                                column.getNome(),
                                countsByColumn.getOrDefault(column.getId(), 0L)))
                        .toList();

        return new Context(memberCount, groupTasks.size(), tasksByColumn);
    }

    public record Context(
            long memberCount, long taskCount, List<GroupMetric.Item> tasksByColumn
    ) {}
}
