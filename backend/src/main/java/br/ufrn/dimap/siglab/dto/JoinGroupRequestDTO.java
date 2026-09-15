package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinGroupRequestDTO {

    @NotBlank(message = "O token de convite é obrigatório")
    private String inviteToken;

    @NotBlank(message = "O e-mail do usuário é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    private String userEmail;
}
