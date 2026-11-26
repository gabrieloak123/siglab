package br.ufrn.dimap.siglab.model;

import java.util.List;
import java.util.function.Function;

public record GroupMetric<C>(String id, String label, Function<C, Value> calculate) {
    public sealed interface Value permits Total, Breakdown {}

    public record Total(long value) implements Value {}

    public record Breakdown(List<Item> items) implements Value {}

    public record Item(String id, String label, long value) {}
}
