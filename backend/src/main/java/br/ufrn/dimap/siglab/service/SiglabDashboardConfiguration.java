package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.model.Coluna;
import br.ufrn.dimap.siglab.model.GroupMetric;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.model.Tarefa;
import br.ufrn.dimap.siglab.repository.ColunaRepository;
import br.ufrn.dimap.siglab.repository.PostRepository;
import br.ufrn.dimap.siglab.repository.TarefaRepository;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiglabDashboardConfiguration {

    @Bean
    GroupDashboardDefinition<Context> siglabDashboard(
            User_ResearchGroupRepository memberships,
            ColunaRepository columns,
            TarefaRepository tasks, PostRepository posts
    ) {
        return new GroupDashboardDefinition<>(
                groupId -> loadContext(groupId, memberships, columns, tasks, posts),
                List.of(
                        new GroupMetric<>("members", "Total de membros",
                                context -> new GroupMetric.Total(context.memberCount())),
                        new GroupMetric<>("tasks", "Total de tarefas",
                                context -> new GroupMetric.Total(context.taskCount())),
                        new GroupMetric<>("tasks-by-column", "Tarefas por coluna",
                                context -> new GroupMetric.Breakdown(context.tasksByColumn())),
                        new GroupMetric<>("members-by-completed-tasks", "Membros com mais tarefas concluídas",
                                context -> new GroupMetric.Breakdown(context.membersByCompletedTasks())),
                        new GroupMetric<>("posts-by-type", "Posts por tipo",
                                context -> new GroupMetric.Breakdown(context.postsByType())),
                        new GroupMetric<>("post-tags-cloud", "Tags dos posts",
                                context -> new GroupMetric.Breakdown(context.postTags())),
                        new GroupMetric<>("member-interests-cloud", "Interesses dos membros",
                                context -> new GroupMetric.Breakdown(context.memberInterests()))
                )
        );
    }

    private Context loadContext(
            String groupId,
            User_ResearchGroupRepository memberships,
            ColunaRepository columns,
            TarefaRepository tasks, PostRepository posts
    ) {
        List<User> members = memberships.findUsersByGroupId(groupId);
        List<Coluna> groupColumns = columns.findByIdGrupoPesquisaOrderByOrdemAsc(groupId);
        List<Tarefa> groupTasks = tasks.findByIdGrupo(groupId);
        Map<Long, Long> countsByColumn = groupTasks.stream()
                .collect(Collectors.groupingBy(Tarefa::getIdColuna, Collectors.counting()));

        List<GroupMetric.Item> tasksByColumn =
                groupColumns.stream()
                        .map(column -> new GroupMetric.Item(
                                column.getId().toString(),
                                column.getNome(),
                                countsByColumn.getOrDefault(column.getId(), 0L)))
                        .toList();

        List<Post> groupPosts = posts.findByGroupIdOrderByCreatedAtDesc(groupId);
        return new Context(members.size(), groupTasks.size(), tasksByColumn,
                rankMembers(members, groupColumns, groupTasks),
                countPostsByType(groupPosts),
                countTerms(groupPosts.stream().map(Post::getTags)),
                countTerms(members.stream().map(User::getInterestAreas)));
    }

    private List<GroupMetric.Item> rankMembers(
            List<User> members, List<Coluna> columns, List<Tarefa> tasks
    ) {
        var completedColumnIds = columns.stream()
                .filter(column -> "Concluído".equals(column.getNome()))
                .map(Coluna::getId)
                .collect(Collectors.toSet());

        var completedByMember = tasks.stream()
                .filter(task -> completedColumnIds.contains(task.getIdColuna()))
                .map(Tarefa::getIdAtribuidoA)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        return members.stream()
                .filter(member -> completedByMember.containsKey(member.getId()))
                .map(member -> new GroupMetric.Item(member.getId().toString(),
                        member.getName(), completedByMember.get(member.getId())))
                .sorted(Comparator.comparingLong(GroupMetric.Item::value).reversed()
                        .thenComparing(GroupMetric.Item::label)
                        .thenComparing(GroupMetric.Item::id))
                .toList();
    }

    private List<GroupMetric.Item> countPostsByType(List<Post> posts) {
        long articles = posts.stream()
                .filter(post -> "ARTIGO".equalsIgnoreCase(post.getPostType()))
                .count();
        return List.of(
                new GroupMetric.Item("ARTIGO", "Artigo", articles),
                new GroupMetric.Item("OUTROS", "Outros", posts.size() - articles));
    }

    private List<GroupMetric.Item> countTerms(Stream<String[]> termsBySource) {
        return termsBySource
                .filter(Objects::nonNull)
                .flatMap(terms -> Arrays.stream(terms)
                        .filter(Objects::nonNull)
                        .map(term -> term.strip().toLowerCase(Locale.ROOT))
                        .filter(term -> !term.isEmpty())
                        .distinct())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new GroupMetric.Item(entry.getKey(), entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(GroupMetric.Item::value).reversed()
                        .thenComparing(GroupMetric.Item::label))
                .toList();
    }

    public record Context(
            long memberCount, long taskCount, List<GroupMetric.Item> tasksByColumn,
            List<GroupMetric.Item> membersByCompletedTasks,
            List<GroupMetric.Item> postsByType,
            List<GroupMetric.Item> postTags,
            List<GroupMetric.Item> memberInterests
    ) {}
}
