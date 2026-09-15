package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para a requisição de ingresso em um grupo de pesquisa utilizando a hash de convite.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinGroupRequestDTO {

    /**
     * O token/hash de convite recebido pelo usuário.
     */
    @NotBlank(message = "O token de convite é obrigatório")
    private String inviteToken;

    /**
     * E-mail do usuário que deseja ingressar no grupo.
     */
    @NotBlank(message = "O e-mail do usuário é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    private String userEmail;
}
