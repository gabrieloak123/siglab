package br.ufrn.dimap.siglab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveTarefaRequestDTO {

    @NotNull(message = "O ID da coluna de destino e obrigatorio")
    @JsonProperty("id_coluna_destino")
    private Long idColunaDestino;
}
