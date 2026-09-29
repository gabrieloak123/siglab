package br.ufrn.dimap.siglab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estado {

    private String id;

    @JsonProperty("id_grupo_pesquisa")
    private String idGrupoPesquisa;

    @Builder.Default
    private List<Coluna> colunas = new ArrayList<>();
}
