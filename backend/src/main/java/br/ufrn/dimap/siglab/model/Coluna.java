package br.ufrn.dimap.siglab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kanban_columns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coluna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String nome;

    @NotNull
    @Column(name = "column_order", nullable = false)
    private Integer ordem;

    @NotBlank
    @Column(name = "research_group_id", nullable = false)
    @JsonProperty("id_grupo_pesquisa")
    private String idGrupoPesquisa;

    @Transient
    @Builder.Default
    private List<Tarefa> tarefas = new ArrayList<>();
}
