package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload de atualização de um post existente por um líder de grupo de pesquisa.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePostRequestDTO {

    @NotBlank(message = "O título do post é obrigatório")
    @Size(max = 255, message = "O título do post deve ter no máximo 255 caracteres")
    private String title;

    private String description;

    @NotBlank(message = "O conteúdo do post em Markdown é obrigatório")
    private String content;

    private String[] tags;

    @Size(max = 500, message = "O nome do arquivo deve ter no máximo 500 caracteres")
    private String filePath;
}
