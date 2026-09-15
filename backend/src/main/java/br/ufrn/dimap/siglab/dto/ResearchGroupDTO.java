package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transferência de dados de Grupo de Pesquisa nas requisições da API REST.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchGroupDTO {

    /**
     * Identificador em texto do grupo de pesquisa.
     */
    @NotBlank(message = "O group_id é obrigatório")
    private String group_id;

    /**
     * Nome do grupo de pesquisa.
     */
    @NotBlank(message = "O nome é obrigatório")
    private String name;

    /**
     * Array de áreas/linhas de pesquisa do grupo.
     */
    private String[] research_fields;
}
