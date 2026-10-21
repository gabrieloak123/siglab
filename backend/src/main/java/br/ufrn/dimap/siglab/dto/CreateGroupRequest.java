package br.ufrn.dimap.siglab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(@JsonProperty("group_id") @NotBlank @Size(max=255) String groupId,
                                 @NotBlank @Size(max=255) String name,
                                 @JsonProperty("research_fields") @NotNull @Size(min=3, max=3) String[] researchFields) {}
