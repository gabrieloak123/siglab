package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.model.GroupMetric;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.function.Function;

public record GroupDashboardDefinition<C>(
        Function<String, C> loadContext,
        List<GroupMetric<C>> metrics
) {
    public GroupDashboardDefinition {
        metrics = List.copyOf(metrics);
    }

    public Dashboard calculate(String groupId) {
        C context = loadContext.apply(groupId);
        return new Dashboard(metrics.stream()
                .map(metric -> evaluate(metric, context))
                .toList());
    }

    private MetricResult evaluate(GroupMetric<C> metric, C context) {
        return switch (metric.calculate().apply(context)) {
            case GroupMetric.Total total ->
                    new MetricResult(metric.id(), metric.label(), "total",
                            total.value(), null);
            case GroupMetric.Breakdown breakdown ->
                    new MetricResult(metric.id(), metric.label(), "breakdown",
                            null, List.copyOf(breakdown.items()));
        };
    }

    public record Dashboard(List<MetricResult> metrics) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MetricResult(
            String id, String label, String type, Long value, List<GroupMetric.Item> items
    ) {}
}
