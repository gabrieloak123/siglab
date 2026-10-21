package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DiscoverySelectionRequest(@NotNull @Size(min=1, max=30) List<@NotNull @Positive Long> postIds) {}
