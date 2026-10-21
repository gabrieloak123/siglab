package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.SendEmailRequestDTO;
import br.ufrn.dimap.siglab.service.EmailService;
import br.ufrn.dimap.siglab.service.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST responsável pelo envio de e-mails pelo SIGLab.
 * <p>
 * O endpoint exige que o usuário esteja autenticado (sessão ativa) para evitar
 * que a API seja usada como relay de spam.
 * </p>
 *
 */
@RestController
@RequestMapping("/api/emails")
@CrossOrigin(origins = "*")
public class EmailController {

    private final EmailService emailService;
    private final SessionUser sessionUser;

    public EmailController(EmailService emailService, SessionUser sessionUser) {
        this.emailService = emailService;
        this.sessionUser = sessionUser;
    }

    /**
     * POST /api/emails - Envia um e-mail em texto puro para o destinatário informado (apenas usuários autenticados).
     * <p>
     * Em caso de falha no envio (SMTP indisponível, credenciais inválidas etc.),
     * uma {@link ExternalServiceException} é retornada como HTTP 503 pelo tratador global.
     * </p>
     *
     * @param request DTO {@link SendEmailRequestDTO} contendo destinatário, assunto e corpo do e-mail
     * @return {@link ResponseEntity} sem conteúdo (204 No Content)
     */
    @PostMapping
    public ResponseEntity<Void> sendEmail(@Valid @RequestBody SendEmailRequestDTO request,
                                          HttpServletRequest servletRequest) {
        sessionUser.require(servletRequest);
        emailService.sendEmail(request.getTo(), request.getSubject(), request.getBody());
        return ResponseEntity.noContent().build();
    }
}
