package br.ufrn.dimap.siglab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kanban_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank
    @Column(name = "group_id", nullable = false)
    @JsonProperty("id_grupo")
    private String idGrupo;

    @NotNull
    @Column(name = "created_by_id", nullable = false)
    @JsonProperty("id_criado_por")
    private Long idCriadoPor;

    @Column(name = "assigned_to_id")
    @JsonProperty("id_atribuido_a")
    private Long idAtribuidoA;

    @NotBlank
    @Column(name = "title", nullable = false)
    private String titulo;

    @Column(name = "description", columnDefinition = "TEXT")
    @JsonProperty("descricao")
    private String descricao;

    @Column(name = "tags")
    private String[] tags;

    @NotNull
    @Column(name = "column_id", nullable = false)
    @JsonProperty("id_coluna")
    private Long idColuna;
}
