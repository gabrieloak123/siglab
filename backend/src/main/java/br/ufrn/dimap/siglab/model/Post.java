package br.ufrn.dimap.siglab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa um Post (publicação) de um Grupo de Pesquisa.
 * <p>
 * O conteúdo do post é escrito em Markdown e armazenado na coluna {@code content};
 * a coluna {@code file_path} guarda apenas o nome do arquivo Markdown original,
 * enviado pelo líder no momento da publicação.
 * </p>
 *
 */
@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    /**
     * Identificador único auto-incremental do post.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    /**
     * Identificador do grupo de pesquisa dono do post.
     */
    @NotBlank
    @Column(name = "group_id", nullable = false)
    private String groupId;

    /**
     * Identificador do usuário que publicou o post (líder do grupo).
     * Pode ser nulo caso o usuário publicador seja excluído do sistema.
     */
    @Column(name = "publisher_user_id")
    private Long publisherUserId;

    /**
     * Título do post.
     */
    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Descrição/resumo do post, digitada pelo líder.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Conteúdo do post em Markdown, enviado a partir do arquivo arrastado.
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Array com as tags de classificação do post.
     */
    @Column(name = "tags")
    private String[] tags;

    /**
     * Nome do arquivo Markdown enviado pelo líder, mantido apenas como referência.
     */
    @Column(name = "file_path")
    private String filePath;

    /**
     * Data e hora de publicação do post.
     */
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}
