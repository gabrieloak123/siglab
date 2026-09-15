package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchGroupDTO {

    @NotBlank(message = "O group_id é obrigatório")
    private String group_id;

    @NotBlank(message = "O nome é obrigatório")
    private String name;

    private String[] research_fields;
}
