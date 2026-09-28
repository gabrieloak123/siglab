package br.ufrn.dimap.siglab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTarefaRequestDTO {

    @NotBlank(message = "O titulo e obrigatorio")
    private String titulo;

    private String descricao;

    private String[] tags;

    @JsonProperty("id_atribuido_a")
    private Long idAtribuidoA;
}
