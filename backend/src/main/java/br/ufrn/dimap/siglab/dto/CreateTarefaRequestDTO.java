package br.ufrn.dimap.siglab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTarefaRequestDTO {

    @NotNull(message = "O ID do criador e obrigatorio")
    @JsonProperty("id_criado_por")
    private Long idCriadoPor;

    @JsonProperty("id_atribuido_a")
    private Long idAtribuidoA;

    @NotNull(message = "O ID da coluna e obrigatorio")
    @JsonProperty("id_coluna")
    private Long idColuna;

    @NotBlank(message = "O titulo e obrigatorio")
    private String titulo;

    private String descricao;

    private String[] tags;
}
