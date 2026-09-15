package br.ufrn.dimap.siglab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transferência de dados de Usuário nas requisições da API REST.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    /**
     * Identificador do usuário (somente leitura no cadastro).
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "ID auto-incremental gerado pelo banco de dados")
    private Long id;

    /**
     * E-mail único de identificação e acesso.
     */
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    /**
     * Senha do usuário.
     */
    @NotBlank(message = "A senha é obrigatória")
    private String password;

    /**
     * Nome completo do usuário.
     */
    @NotBlank(message = "O nome é obrigatório")
    private String name;

    /**
     * Array de áreas de interesse.
     */
    private String[] interest_areas;

    /**
     * Array com os IDs dos grupos de pesquisa dos quais o usuário é membro.
     */
    private String[] groups;
}
