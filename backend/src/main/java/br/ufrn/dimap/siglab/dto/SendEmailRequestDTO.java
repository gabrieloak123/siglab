package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para a requisição de envio de e-mail através do servidor SMTP do SIGLab.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendEmailRequestDTO {

    /**
     * Endereço de e-mail do destinatário.
     */
    @NotBlank(message = "O e-mail do destinatário é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    private String to;

    /**
     * Assunto do e-mail.
     */
    @NotBlank(message = "O assunto é obrigatório")
    private String subject;

    /**
     * Corpo do e-mail (texto puro).
     */
    @NotBlank(message = "O corpo do e-mail é obrigatório")
    private String body;
}
